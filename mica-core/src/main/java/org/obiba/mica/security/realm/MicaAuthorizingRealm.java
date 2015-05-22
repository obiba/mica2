package org.obiba.mica.security.realm;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.PermissionUtils;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.event.SubjectAclUpdatedEvent;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;

@Component
public class MicaAuthorizingRealm extends AuthorizingRealm implements RolePermissionResolver {

  @Inject
  private SubjectAclService subjectAclService;

  private final RolePermissionResolver rolePermissionResolver = new GroupPermissionResolver();



  @Override
  public boolean supports(AuthenticationToken token) {
    // This realm is not used for authentication
    return false;
  }

  /**
   * Overridden because the OpalSecurityManager sets {@code this} as the {@code RolePermissionResolver} on all configured
   * realms. This results the following object graph:
   * <p/>
   * <pre>
   * AuthorizingReam.rolePermissionResolver -> MicaAuthorizingRealm (this)
   *      ^
   *      |
   * MicaAuthorizingRealm.rolePermissionResolver -> GroupPermissionResolver
   *
   * <pre>
   * By overriding this method, we prevent an infinite loop from occurring when
   * {@code getRolePermissionResolver().resolvePermissionsInRole()} is called.
   */
  @Override
  public RolePermissionResolver getRolePermissionResolver() {
    return rolePermissionResolver;
  }

  //
  // AuthorizingRealm
  //

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    List<String> perms = loadUserPermissions(principals);
    if(perms == null|| perms.isEmpty()) return null;
    SimpleAuthorizationInfo sai = new SimpleAuthorizationInfo();
    sai.setStringPermissions(ImmutableSet.copyOf(perms));
    return sai;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    // This realm is not used for authentication
    return null;
  }

  @Override
  protected void afterCacheManagerSet() {
    super.afterCacheManagerSet();
    if(isAuthorizationCachingEnabled()) {
      CacheManager cacheManager = getCacheManager();
      //rolePermissionCache = cacheManager.getCache(getAuthorizationCacheName() + "_role");
    }
  }

  @Override
  protected Object getAuthorizationCacheKey(PrincipalCollection principals) {
    return SubjectAcl.Type.USER.subjectFor(principals.getPrimaryPrincipal().toString());
  }

  //
  // RolePermissionResolver
  //

  @Override
  public Collection<Permission> resolvePermissionsInRole(String roleString) {
    return rolePermissionResolver.resolvePermissionsInRole(roleString);
  }

  //
  // Events
  //

  @Subscribe
  public void onSubjectAclUpdate(SubjectAclUpdatedEvent event) {
    if (isCachingEnabled()) {
      getAuthorizationCache().remove(event.getSubject());
    }
  }

  //
  // Private methods
  //

  private List<String> loadUserPermissions(PrincipalCollection principals) {
    return loadSubjectPermissions(principals.getPrimaryPrincipal().toString(), SubjectAcl.Type.USER);
  }

  private List<String> loadSubjectPermissions(String name, SubjectAcl.Type type) {
    return subjectAclService.find(name, type).stream().map(
      SubjectAcl::getPermission).collect(Collectors.toList());
  }

  //
  // Inner classes
  //

  private final class GroupPermissionResolver implements RolePermissionResolver {

    @Override
    public Collection<Permission> resolvePermissionsInRole(String roleString) {
      // built-in permissions
      switch(roleString) {
        case Roles.MICA_ADMIN:
          return PermissionUtils.resolveDelimitedPermissions("*", getPermissionResolver());
        case Roles.MICA_REVIEWER:
          return PermissionUtils
            .resolveDelimitedPermissions("/draft:EDIT,/draft:PUBLISH,/files:UPLOAD", getPermissionResolver());
        case Roles.MICA_EDITOR:
          return PermissionUtils.resolveDelimitedPermissions("/draft:EDIT,/files:UPLOAD", getPermissionResolver());
        case Roles.MICA_DAO:
          return PermissionUtils
            .resolveDelimitedPermissions("/data-access-request:EDIT,/files:UPLOAD", getPermissionResolver());
        case Roles.MICA_USER:
          return null;
      }
      // other groups
      List<String> permissions = loadSubjectPermissions(roleString, SubjectAcl.Type.GROUP);
      return PermissionUtils.resolvePermissions(permissions, getPermissionResolver());
    }
  }

}
