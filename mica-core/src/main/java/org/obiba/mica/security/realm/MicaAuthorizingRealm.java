package org.obiba.mica.security.realm;

import java.util.Arrays;
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
import org.obiba.mica.security.PermissionsUtils;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.event.SubjectAclUpdatedEvent;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;

@Component
public class MicaAuthorizingRealm extends AuthorizingRealm implements RolePermissionResolver {

  private static final String[] ALL_RESOURCES = { "network", "study", "study-dataset", "harmonization-dataset" };

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
    if(perms == null || perms.isEmpty()) return null;
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
    if(isCachingEnabled()) {
      if(event.hasSubject()) getAuthorizationCache().remove(event.getSubject());
      else getAuthorizationCache().clear();
    }
  }

  //
  // Private methods
  //

  private List<String> loadUserPermissions(PrincipalCollection principals) {
    return loadSubjectPermissions(principals.getPrimaryPrincipal().toString(), SubjectAcl.Type.USER);
  }

  private List<String> loadSubjectPermissions(String name, SubjectAcl.Type type) {
    return subjectAclService.findBySubject(name, type).stream().map(SubjectAcl::getPermission)
      .collect(Collectors.toList());
  }

  //
  // Inner classes
  //

  private final class GroupPermissionResolver implements RolePermissionResolver {

    @Override
    public Collection<Permission> resolvePermissionsInRole(String roleString) {
      List<String> permissions = loadSubjectPermissions(roleString, SubjectAcl.Type.GROUP);
      // built-in permissions
      Collection<Permission> perms;
      switch(roleString) {
        case Roles.MICA_ADMIN:
          return mergePermissions("*", permissions);
        case Roles.MICA_REVIEWER:
          // all permissions: edition and publication
          perms = mergePermissions("/files:UPLOAD", permissions);
          Arrays.stream(ALL_RESOURCES).forEach(e -> {
            perms.addAll(toPermissions(String.format("/draft/%s", e)));
            perms.addAll(toPermissions(String.format("/draft/file:*:/%s", e)));
          });
          return perms;
        case Roles.MICA_EDITOR:
          // all edition permissions
          perms = mergePermissions("/files:UPLOAD", permissions);
          Arrays.stream(ALL_RESOURCES).forEach(e -> {
            PermissionsUtils.EDITOR_ACTIONS.forEach(o -> {
              perms.addAll(toPermissions(String.format("/draft/%s:%s", e, o)));
              perms.addAll(toPermissions(String.format("/draft/file:%s:/%s", o, e)));
            });
          });
          return perms;
        case Roles.MICA_DAO:
        case Roles.MICA_USER:
          return mergePermissions("/data-access-request:ADD,/files:UPLOAD", permissions);
      }
      // other groups
      return PermissionUtils.resolvePermissions(permissions, getPermissionResolver());
    }

    private Collection<Permission> mergePermissions(String delimitedPermissions, Collection<String> permissions) {
      Collection<Permission> perms = toPermissions(delimitedPermissions);
      perms.addAll(PermissionUtils.resolvePermissions(permissions, getPermissionResolver()));
      return perms;
    }

    private Collection<Permission> toPermissions(String delimitedPermissions) {
      return PermissionUtils.resolveDelimitedPermissions(delimitedPermissions, getPermissionResolver());
    }
  }

}
