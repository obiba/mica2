package org.obiba.mica.security.service;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.event.FileDeletedEvent;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.event.NetworkDeletedEvent;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.event.ResourceDeletedEvent;
import org.obiba.mica.security.event.SubjectAclUpdatedEvent;
import org.obiba.mica.security.repository.SubjectAclRepository;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.codahale.metrics.annotation.Timed;
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
  private MicaConfigService micaConfigService;

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

  /**
   * Get all permissions for the given resource and instance and for any subject.
   *
   * @param resource
   * @param instance
   * @return
   */
  public List<SubjectAcl> findByResourceInstance(String resource, String instance) {
    return subjectAclRepository.findByResourceAndInstance(resource, encode(instance),
      new Sort(new Sort.Order(Sort.Direction.DESC, "type"), new Sort.Order(Sort.Direction.ASC, "principal")));
  }

  /**
   * Return if the permission applies to the current user.
   *
   * @param resource
   * @param action
   * @return
   */
  @Timed
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
  @Timed
  public boolean isPermitted(@NotNull String resource, @NotNull String action, @Nullable String instance) {
    return SecurityUtils.getSubject()
      .isPermitted(resource + ":" + action + (Strings.isNullOrEmpty(instance) ? "" : ":" + encode(instance)));
  }

  /**
   * Check if current user has the given permission.
   *
   * @param resource
   * @param action
   * @throws AuthorizationException
   */
  @Timed
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
  @Timed
  public void checkPermission(@NotNull String resource, @NotNull String action,
    @Nullable String instance) throws AuthorizationException {
    SecurityUtils.getSubject()
      .checkPermission(resource + ":" + action + (Strings.isNullOrEmpty(instance) ? "" : ":" + encode(instance)));
  }

  /**
   * Check if published documents access is to be checked.
   *
   * @return
   */
  public boolean isOpenAccess() {
    return micaConfigService.getConfig().isOpenAccess();
  }

  /**
   * Verify the {@link org.obiba.mica.micaConfig.domain.MicaConfig} open access property before evaluating the
   * "VIEW" permission for the current user.
   *
   * @param resource Resource is expected to apply to a published document
   * @param instance
   * @return
   */
  @Timed
  public boolean isAccessible(@NotNull String resource, @Nullable String instance) {
    return micaConfigService.getConfig().isOpenAccess() || isPermitted(resource, "VIEW", instance);
  }

  /**
   * Verify the {@link org.obiba.mica.micaConfig.domain.MicaConfig} open access property before evaluating the
   * "VIEW" permission for the current user.
   *
   * @param resource Resource is expected to apply to a published document
   * @param instance
   * @return
   */
  @Timed
  public void checkAccess(@NotNull String resource, @Nullable String instance) {
    if(micaConfigService.getConfig().isOpenAccess()) return;
    checkPermission(resource, "VIEW", instance);
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
  synchronized public void addSubjectPermission(@NotNull SubjectAcl.Type type, @NotNull String principal,
    @NotNull String resource, @Nullable String action, @Nullable String instance) {
    List<SubjectAcl> acls = subjectAclRepository
      .findByPrincipalAndTypeAndResourceAndInstance(principal, type, resource, encode(instance));
    SubjectAcl acl;
    if(acls == null || acls.isEmpty()) {
      acl = SubjectAcl.newBuilder(principal, type).resource(resource).action(action).instance(encode(instance)).build();
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
    subjectAclRepository.findByPrincipalAndTypeAndResourceAndInstance(principal, type, resource, encode(instance))
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
      .delete(subjectAclRepository.findByResourceAndInstance(event.getResource(), encode(event.getInstance())));
    // delete children acls, i.e. acls which resource name starts with regex "<resource>/<instance>/.+"
    subjectAclRepository
      .delete(subjectAclRepository.findByResourceStartingWith(event.getResource() + "/" + encode(event.getInstance()) +
        "/.+"));
  }

  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {
    removeInstance("/study", event.getPersistable().getId());
  }

  @Async
  @Subscribe
  public void networkDeleted(NetworkDeletedEvent event) {
    removeInstance("/network", event.getPersistable().getId());
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    removeInstance(event.isStudyDataset() ? "/study-dataset" : "/harmonization-dataset",
      event.getPersistable().getId());
  }

  @Async
  @Subscribe
  public void fileDeleted(FileDeletedEvent event) {
    subjectAclRepository
      .delete(subjectAclRepository.findByResourceAndInstance("/file", encode(event.getPersistable().getFullPath())));
    subjectAclRepository.delete(
      subjectAclRepository.findByResourceAndInstanceRegex("/file", "^" + encode(event.getPersistable().getFullPath()) +
        "/"));
    subjectAclRepository.delete(
      subjectAclRepository.findByResourceAndInstance("/draft/file", encode(event.getPersistable().getFullPath())));
    subjectAclRepository.delete(subjectAclRepository
      .findByResourceAndInstanceRegex("/draft/file", "^" + encode(event.getPersistable().getFullPath()) + "/"));
  }

  //
  // Private methods
  //

  /**
   * Remove all access controls associated to the instance: draft and published, entity and files.
   *
   * @param resource
   * @param instance
   */
  private void removeInstance(String resource, String instance) {
    // entity, published and draft
    subjectAclRepository.delete(subjectAclRepository.findByResourceAndInstance(resource, encode(instance)));
    subjectAclRepository.delete(subjectAclRepository.findByResourceAndInstance("/draft" + resource, encode(instance)));

    // file and descendants, published and draft
    subjectAclRepository
      .delete(subjectAclRepository.findByResourceAndInstance("/file", resource + "/" + encode(instance)));
    subjectAclRepository.delete(
      subjectAclRepository.findByResourceAndInstanceRegex("/file", "^" + resource + "/" + encode(instance) + "/"));
    subjectAclRepository
      .delete(subjectAclRepository.findByResourceAndInstance("/draft/file", resource + "/" + encode(instance)));
    subjectAclRepository.delete(subjectAclRepository
      .findByResourceAndInstanceRegex("/draft/file", "^" + resource + "/" + encode(instance) + "/"));
  }

  public void removeSubjectPermission(@NotNull SubjectAcl.Type type, @NotNull String principal,
    @NotNull String resource, @NotNull String action, @NotNull String instance) {
    subjectAclRepository.findByPrincipalAndTypeAndResourceAndInstance(principal, type, resource, encode(instance))
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

  private String encode(String instance) {
    return FileUtils.encode(instance);
  }
}
