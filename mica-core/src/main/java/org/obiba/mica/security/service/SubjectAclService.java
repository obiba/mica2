/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.security.service;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.event.DataAccessAmendmentDeletedEvent;
import org.obiba.mica.access.event.DataAccessRequestDeletedEvent;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.event.FileDeletedEvent;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.event.NetworkDeletedEvent;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.event.ResourceDeletedEvent;
import org.obiba.mica.security.event.SubjectAclUpdatedEvent;
import org.obiba.mica.security.repository.SubjectAclRepository;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Access control lists management: add, remove and check permissions on resources.
 */
@Service
public class SubjectAclService {

  private static final Logger logger = LoggerFactory.getLogger(SubjectAclService.class);

  private static final DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd");

  @Inject
  private SubjectAclRepository subjectAclRepository;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private EventBus eventBus;

  private Cache<String, Boolean> permissionCache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(10, TimeUnit.SECONDS).build();

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

  public boolean hasMicaRole() {
    return Stream.of(Roles.MICA_DAO, Roles.MICA_ADMIN, Roles.MICA_EDITOR, Roles.MICA_REVIEWER, Roles.MICA_USER)
      .anyMatch(SecurityUtils.getSubject()::hasRole);
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
    String perm = resource + ":" + action + (Strings.isNullOrEmpty(instance) ? "" : ":" + encode(instance));
    Subject subject = SecurityUtils.getSubject();
    if (subject.getPrincipal() == null) logger.warn("Checking permissions for a 'null' principal");
    String permKey = subject.getPrincipal() + ":" + perm;
    try {
      return permissionCache.get(permKey, () -> subject.isPermitted(perm));
    } catch (ExecutionException e) {
      return subject.isPermitted(perm);
    }
  }

  @Timed
  public void checkPermission(@NotNull String resource, @NotNull String action) throws AuthorizationException {
    checkPermission(resource, action, null);
  }

  @Timed
  public void checkPermission(@NotNull String resource, @NotNull String action,
                              @Nullable String instance) throws AuthorizationException {
    checkPermission(resource, action, instance, null);
  }

  /**
   * Check the permission (action on a resource). If a key is provided and is valid, the permission check is by-passed.
   * If the provided key is not valid, permission check is applied.
   *
   * @param resource
   * @param action
   * @param instance any instances if null
   * @param shareKey
   * @throws AuthorizationException
   */
  @Timed
  public void checkPermission(@NotNull String resource, @NotNull String action,
    @Nullable String instance, @Nullable String shareKey) throws AuthorizationException {

    if (isAccessWithShareKeyApplicable(resource, action, instance, shareKey)) {
      if (validateShareKey(shareKey, draftRessourcesValidatorForShareKeyAccess(resource, instance)))
        return;
    }
    SecurityUtils.getSubject()
      .checkPermission(resource + ":" + action + (Strings.isNullOrEmpty(instance) ? "" : ":" + encode(instance)));
  }


  /**
   * Create share key usable to give access to draft resources.
   *
   * @param content used to define access granted by the key
   * @param expire (Optional) expiration date
   * @return
   */
  public String createShareKey(String content, String expire) {
    if(!Strings.isNullOrEmpty(expire)) {
      try {
        iso8601.parse(expire);
      } catch (ParseException e) {
        throw new IllegalArgumentException("Not a valid expiration date");
      }
    }

    logger.debug("createShareKey, encrypting...");
    return micaConfigService.encrypt(String.format("%s|%s|%s",
      content, Strings.isNullOrEmpty(expire) ? "" : expire, SecurityUtils.getSubject().getPrincipal()));
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
    broadcastSubjectAclUpdateEvent(type, principal);
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
    broadcastSubjectAclUpdateEvent(type, principal);
  }

  //
  // Events handling
  //

  private void removeResourcePermissions(String resource, String instance) {
    // delete specific acls
    subjectAclRepository.delete(subjectAclRepository.findByResourceAndInstance(resource, encode(instance)));
    // delete children acls, i.e. acls which resource name starts with regex "<resource>/<instance>/.+"

    String resourcePattern = resource + (Strings.isNullOrEmpty(instance) ? "" : "/" + encode(instance) + "/.+");
    subjectAclRepository.delete(subjectAclRepository.findByResourceStartingWith(resourcePattern));
  }

  /**
   * Remove all the permissions associated to the resource.
   *
   * @param event
   */
  @Subscribe
  public void onResourceDeleted(ResourceDeletedEvent event) {
    removeResourcePermissions(event.getResource(), event.getInstance());
  }

  @Async
  @Subscribe
  public void dataAccessRequestDeleted(DataAccessRequestDeletedEvent event) {
    DataAccessRequest request = event.getPersistable();
    String resource  = "/data-access-request";
    String id  = request.getId();
    removeResourcePermissions(resource, id);
    removeResourcePermissions(resource + "/" + id, "_status");
    removeResourcePermissions(resource + "/" + id+ "/amendment", null);
    removeResourcePermissions(resource + "/" + id + "/_attachments", null);
    removeResourcePermissions(resource + "/" + id+ "/comment", null);
  }

  @Async
  @Subscribe
  public void dataAccessAmendmentDeleted(DataAccessAmendmentDeletedEvent event) {
    DataAccessAmendment amendment = event.getPersistable();
    String resource  = String.format("/data-access-request/%s/amendment", amendment.getParentId());
    String id  = amendment.getId();
    removeResourcePermissions(resource, id);
    removeResourcePermissions(resource + "/" + id, "_status");
  }

  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {
    BaseStudy persistable = event.getPersistable();
    removeInstance(persistable instanceof Study ? "/individual-study" : "/harmonization-study", persistable.getId());
  }

  @Async
  @Subscribe
  public void networkDeleted(NetworkDeletedEvent event) {
    removeInstance("/network", event.getPersistable().getId());
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    removeInstance(event.isStudyDataset() ? "/collected-dataset" : "/harmonized-dataset",
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
    broadcastSubjectAclUpdateEvent(type, principal);
  }

  private void  broadcastSubjectAclUpdateEvent(SubjectAcl.Type type, String principal) {
    permissionCache.invalidateAll();
    permissionCache.cleanUp();

    eventBus.post(new SubjectAclUpdatedEvent(type.subjectFor(principal)));
  }

  private String encode(String instance) {
    return FileUtils.encode(instance);
  }

  private Function<String, Boolean> draftRessourcesValidatorForShareKeyAccess(String resource, String instance) {
    if (resource.equals("/draft/file"))
      return instance.startsWith("/draft") ? instance::startsWith : ("/draft" + instance)::startsWith;
    else
      return (resource + "/" + instance)::startsWith;
  }

  private boolean isAccessWithShareKeyApplicable(String resource, String action, String instance, String shareKey) {
    return shareKey != null && instance != null && "VIEW".equals(action) && resource.startsWith("/draft/");
  }

  private boolean validateShareKey(String key, Function<String, Boolean> contentValidator) {

    String decrypted = micaConfigService.decrypt(key);
    String[] tokens = decrypted.split("\\|");

    if (tokens.length != 3) return false;
    if (isExpired(tokens[1])) return false;

    String grantedPath = tokens[0];
    Boolean isValid = contentValidator.apply(grantedPath);
    if (isValid == null || !isValid)
      return false;

    logger.info("Using share key on file {} provided by {} expiring on {}",
      tokens[0], tokens[2], StringUtils.isEmpty(tokens[1]) ? "<never>" : tokens[1]);
    return true;
  }

  private boolean isExpired(String token) {
    if (!StringUtils.isEmpty(token)) {
      try {
        if (!iso8601.parse(token).after(new Date())) return true;
      } catch (ParseException e) {
        return true;
      }
    }
    return false;
  }
}
