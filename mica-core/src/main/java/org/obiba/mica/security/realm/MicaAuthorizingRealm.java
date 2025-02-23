/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.security.realm;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;

@Component
public class MicaAuthorizingRealm extends AuthorizingRealm implements RolePermissionResolver {

  private static final String[] ALL_RESOURCES = {
    "network",
    "individual-study",
    "harmonization-study",
    "collected-dataset",
    "harmonized-dataset",
    "project"
  };

  private static final Logger logger = LoggerFactory.getLogger(MicaAuthorizingRealm.class);

  @Inject
  private SubjectAclService subjectAclService;

  private final RolePermissionResolver rolePermissionResolver = new GroupPermissionResolver();

  private Cache<String, Collection<Permission>> rolePermissionsCache = CacheBuilder.newBuilder().maximumSize(10000).expireAfterWrite(1, TimeUnit.MINUTES).build();

  private Cache<String, List<String>> subjectPermissionsCache = CacheBuilder.newBuilder().maximumSize(10000).expireAfterWrite(1, TimeUnit.MINUTES).build();

  @Override
  public boolean supports(AuthenticationToken token) {
    // This realm is not used for authentication
    return false;
  }

  /**
   * Overridden because the OpalSecurityManager sets {@code this} as the {@code RolePermissionResolver} on all configured
   * realms. This results the following object graph:
   *
   * {@code
   * AuthorizingReam.rolePermissionResolver -> MicaAuthorizingRealm (this)
   *      ^
   *      |
   * MicaAuthorizingRealm.rolePermissionResolver -> GroupPermissionResolver
   * }
   *
   * By overriding this method, we prevent an infinite loop from occurring when
   * {@code getRolePermissionResolver().resolvePermissionsInRole()} is called.
   */
  @Override
  public RolePermissionResolver getRolePermissionResolver() {
    return rolePermissionResolver;
  }

  /**
   * Forces cache wipe out, for operations requiring real time permission updates (SubjectAclUpdatedEvent
   * is triggerred ASync).
   */
  public void invalidateCache() {
    logger.warn("Invalidating authorization cache");
    if(isCachingEnabled()) {
      getAuthorizationCache().clear();

      subjectPermissionsCache.invalidateAll();
      subjectPermissionsCache.cleanUp();

      rolePermissionsCache.invalidateAll();
      rolePermissionsCache.cleanUp();
    }
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
    invalidateCache();
  }

  //
  // Private methods
  //

  private List<String> loadUserPermissions(PrincipalCollection principals) {
    return loadSubjectPermissions(principals.getPrimaryPrincipal().toString(), SubjectAcl.Type.USER);
  }

  /**
   * Get permissions that apply to the subject and to any subjects of the same type.
   *
   * @param name
   * @param type
   * @return
   */
  private List<String> loadSubjectPermissions(String name, SubjectAcl.Type type) {
    String key = name + ":" + type;
    try {
      return subjectPermissionsCache.get(key, () -> doLoadSubjectPermissions(name, type));
    } catch (ExecutionException e) {
      return doLoadSubjectPermissions(name, type);
    }
  }

  private List<String> doLoadSubjectPermissions(String name, SubjectAcl.Type type) {
    return Stream
        .concat(subjectAclService.findBySubject("*", type).stream(), subjectAclService.findBySubject(name, type).stream())
        .map(SubjectAcl::getPermission).collect(Collectors.toList());
  }

  //
  // Inner classes
  //

  private final class GroupPermissionResolver implements RolePermissionResolver {

    @Override
    public Collection<Permission> resolvePermissionsInRole(String roleString) {
      Collection<Permission> cachedPerms = rolePermissionsCache.getIfPresent(roleString);
      if (cachedPerms != null) return cachedPerms;

      List<String> permissions = loadSubjectPermissions(roleString, SubjectAcl.Type.GROUP);
      // built-in permissions
      Collection<Permission> perms;
      switch(roleString) {
        case Roles.MICA_ADMIN:
          perms = mergePermissions("*", permissions);
          break;
        case Roles.MICA_REVIEWER:
          // all permissions: edition and publication on draft, view on published
          perms = mergePermissions("/files:UPLOAD", permissions);
          Arrays.stream(ALL_RESOURCES).forEach(e -> {
            perms.addAll(toPermissions(String.format("/draft/%s", e)));
            perms.addAll(toPermissions(String.format("/draft/file:*:/%s", e)));
            perms.addAll(toPermissions(String.format("/%s:VIEW", e)));
            perms.addAll(toPermissions(String.format("/file:VIEW:/%s", e)));
          });
          break;
        case Roles.MICA_EDITOR:
          // all edition permissions on draft
          perms = mergePermissions("/files:UPLOAD", permissions);
          Arrays.stream(ALL_RESOURCES).forEach(e -> PermissionsUtils.EDITOR_ACTIONS.forEach(a -> {
            perms.addAll(toPermissions(String.format("/draft/%s:%s", e, a)));
            perms.addAll(toPermissions(String.format("/draft/file:%s:/%s", a, e)));

          }));
          // all view permissions on published
          Arrays.stream(ALL_RESOURCES).forEach(e -> {
            perms.addAll(toPermissions(String.format("/%s:VIEW", e)));
            perms.addAll(toPermissions(String.format("/file:VIEW:/%s", e)));
          });
          break;
        case Roles.MICA_DAO:
          // can view and delete any project and data access requests
          perms = mergePermissions(
              "/private-comment/data-access-request:VIEW,/private-comment/data-access-request:ADD," +
              "/data-access-request:ADD,/data-access-request:VIEW,/data-access-request:DELETE," +
              "/files:UPLOAD,/user:VIEW",
            permissions);
          break;
        case Roles.MICA_USER:
          perms = mergePermissions("/data-access-request:ADD,/files:UPLOAD", permissions);
          break;
        default:
            // other groups
            perms = PermissionUtils.resolvePermissions(permissions, getPermissionResolver());
      }
      rolePermissionsCache.put(roleString, perms);
      return perms;
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
