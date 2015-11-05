package org.obiba.mica.security.service;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.event.ResourceDeletedEvent;
import org.obiba.mica.security.event.SubjectAclUpdatedEvent;
import org.obiba.mica.security.repository.SubjectAclRepository;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Access control lists management: add, remove and check permissions on resources.
 */
@Service
public class SubjectAclService {

  @Inject
  private SubjectAclRepository subjectAclRepository;

  @Inject
  private EventBus eventBus;

  public boolean isCurrentUser(String principal) {
    return SecurityUtils.getSubject().getPrincipal().toString().equals(principal);
  }

  /**
   * Get all permissions for the matching subjects.
   *
   * @param principal
   * @param type
   * @return
   */
  public List<SubjectAcl> findBySubject(String principal, SubjectAcl.Type type) {
    return subjectAclRepository.findByPrincipalAndType(principal, type);
  }

  public List<SubjectAcl> findByResourceInstance(String resource, String instance) {
    return subjectAclRepository.findByResourceAndInstance(resource, instance);
  }

  /**
   * Return if the permission applies to the current user.
   *
   * @param resource
   * @param action
   * @return
   */
  public boolean isPermitted(@NotNull String resource, @NotNull String action) {
    return isPermitted(resource, action, null);
  }

  /**
   * Return if the permission on the given instance applies to the current user.
   *
   * @param resource
   * @param action
   * @param instance any instances if null
   * @return
   */
  public boolean isPermitted(@NotNull String resource, @NotNull String action, @Nullable String instance) {
    return SecurityUtils.getSubject()
      .isPermitted(resource + ":" + action + (Strings.isNullOrEmpty(instance) ? "" : ":" + instance));
  }

  /**
   * Check if current user has the given permission.
   *
   * @param resource
   * @param action
   * @throws AuthorizationException
   */
  public void checkPermission(@NotNull String resource, @NotNull String action) throws AuthorizationException {
    checkPermission(resource, action, null);
  }

  /**
   * Check if current user has the given permission on the given instance.
   *
   * @param resource
   * @param action
   * @param instance any instances if null
   * @throws AuthorizationException
   */
  public void checkPermission(@NotNull String resource, @NotNull String action, @Nullable String instance)
    throws AuthorizationException {
    SecurityUtils.getSubject()
      .checkPermission(resource + ":" + action + (Strings.isNullOrEmpty(instance) ? "" : ":" + instance));
  }

  /**
   * Add a permission for the current user.
   *
   * @param resource
   * @param action multiple actions can be comma-separated
   */
  public void addPermission(@NotNull String resource, @Nullable String action) {
    addPermission(resource, action, null);
  }

  /**
   * Add a permission for the current user on a given instance.
   *
   * @param resource
   * @param action multiple actions can be comma-separated
   * @param instance any instances if null
   */
  public void addPermission(@NotNull String resource, @Nullable String action, @Nullable String instance) {
    addUserPermission(SecurityUtils.getSubject().getPrincipal().toString(), resource, action, instance);
  }

  /**
   * Add a permission for the user principal on a given instance.
   *
   * @param principal
   * @param resource
   * @param action multiple actions can be comma-separated
   * @param instance any instances if null
   */
  public void addUserPermission(@NotNull String principal, @NotNull String resource, @Nullable String action,
    @Nullable String instance) {
    addSubjectPermission(SubjectAcl.Type.USER, principal, resource, action, instance);
  }

  /**
   * Add a permission for the group principal on a given instance.
   *
   * @param principal
   * @param resource
   * @param action multiple actions can be comma-separated
   * @param instance any instances if null
   */
  public void addGroupPermission(@NotNull String principal, @NotNull String resource, @Nullable String action,
    @Nullable String instance) {
    addSubjectPermission(SubjectAcl.Type.GROUP, principal, resource, action, instance);
  }

  /**
   * Add a permission for the subject principal on a given instance.
   *
   * @param type
   * @param principal
   * @param resource
   * @param action
   * @param instance
   */
  public void addSubjectPermission(@NotNull SubjectAcl.Type type, @NotNull String principal, @NotNull String resource,
    @Nullable String action, @Nullable String instance) {
    List<SubjectAcl> acls = subjectAclRepository
      .findByPrincipalAndTypeAndResourceAndInstance(principal, type, resource, instance);
    SubjectAcl acl;
    if(acls == null || acls.isEmpty()) {
      acl = SubjectAcl.newBuilder(principal, type).resource(resource).action(action).instance(instance).build();
      subjectAclRepository.save(acl);
    } else {
      acl = acls.get(0);
      acl.removeActions();
      acl.addAction(action);
    }
    subjectAclRepository.save(acl);
    // inform acls update (for caching)
    eventBus.post(new SubjectAclUpdatedEvent(type.subjectFor(principal)));
  }

  /**
   * Remove a user permission for the current user on a given instance.
   *
   * @param resource
   * @param action multiple actions can be comma-separated
   * @param instance
   */
  public void removePermission(@NotNull String resource, @NotNull String action, @NotNull String instance) {
    removeUserPermission(SecurityUtils.getSubject().getPrincipal().toString(), resource, action, instance);
  }

  /**
   * Remove a user permission for the user principal on a given instance.
   *
   * @param principal
   * @param resource
   * @param action multiple actions can be comma-separated
   * @param instance
   */
  public void removeUserPermission(@NotNull String principal, @NotNull String resource, @NotNull String action,
    @NotNull String instance) {
    removeSubjectPermission(SubjectAcl.Type.USER, principal, resource, action, instance);
  }

  /**
   * Remove a group permission for the group principal on a given instance.
   *
   * @param principal
   * @param resource
   * @param action multiple actions can be comma-separated
   * @param instance
   */
  public void removeGroupPermission(@NotNull String principal, @NotNull String resource, @NotNull String action,
    @NotNull String instance) {
    removeSubjectPermission(SubjectAcl.Type.GROUP, principal, resource, action, instance);
  }

  /**
   * Remove permissions for the subject principal on a given instance.
   *
   * @param type
   * @param principal
   * @param resource
   * @param instance
   */
  public void removeSubjectPermissions(@NotNull SubjectAcl.Type type, @NotNull String principal,
    @NotNull String resource, @Nullable String instance) {
    subjectAclRepository.findByPrincipalAndTypeAndResourceAndInstance(principal, type, resource, instance)
      .forEach(subjectAclRepository::delete);
    // inform acls update (for caching)
    eventBus.post(new SubjectAclUpdatedEvent(type.subjectFor(principal)));
  }

  //
  // Events handling
  //

  /**
   * Remove all the permissions associated to the resource.
   *
   * @param event
   */
  @Subscribe
  public void onResourceDeleted(ResourceDeletedEvent event) {
    // delete specific acls
    subjectAclRepository
      .delete(subjectAclRepository.findByResourceAndInstance(event.getResource(), event.getInstance()));
    // delete children acls, i.e. acls which resource name starts with regex "<resource>/<instance>/.+"
    subjectAclRepository
      .delete(subjectAclRepository.findByResourceStartingWith(event.getResource() + "/" + event.getInstance() + "/.+"));
  }

  //
  // Private methods
  //

  private void removeSubjectPermission(@NotNull SubjectAcl.Type type, @NotNull String principal,
    @NotNull String resource, @NotNull String action, @NotNull String instance) {
    subjectAclRepository.findByPrincipalAndTypeAndResourceAndInstance(principal, type, resource, instance)
      .forEach(acl -> {
        if(acl.hasAction(action)) {
          acl.removeAction(action);
          if(acl.hasActions()) {
            subjectAclRepository.save(acl);
          } else {
            subjectAclRepository.delete(acl);
          }
        }
      });
    // inform acls update (for caching)
    eventBus.post(new SubjectAclUpdatedEvent(type.subjectFor(principal)));
  }
}
