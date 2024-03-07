package org.obiba.mica.access.rest;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import org.apache.shiro.SecurityUtils;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.event.DataAccessConfigUpdatedEvent;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.SchemaFormConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class DataAccessEntityResource<T extends DataAccessEntity> {

  protected static final String LANGUAGE_TAG_UNDETERMINED = "und";

  protected final SubjectAclService subjectAclService;

  protected final SchemaFormConfigService schemaFormConfigService;

  protected final FileStoreService fileStoreService;

  protected final DataAccessConfigService dataAccessConfigService;

  protected final VariableSetService variableSetService;

  protected final DataAccessRequestUtilService dataAccessRequestUtilService;

  protected abstract DataAccessEntityService<T> getService();

  protected abstract int getFormLatestRevision();

  abstract String getResourcePath();

  public DataAccessEntityResource(
    SubjectAclService subjectAclService,
    FileStoreService fileStoreService,
    DataAccessConfigService dataAccessConfigService,
    VariableSetService variableSetService,
    DataAccessRequestUtilService dataAccessRequestUtilService,
    SchemaFormConfigService schemaFormConfigService) {
    this.subjectAclService = subjectAclService;
    this.fileStoreService = fileStoreService;
    this.dataAccessConfigService = dataAccessConfigService;
    this.variableSetService = variableSetService;
    this.dataAccessRequestUtilService = dataAccessRequestUtilService;
    this.schemaFormConfigService = schemaFormConfigService;
  }

  @Subscribe
  public void onDataAccessFormUpdate(DataAccessConfigUpdatedEvent event) {
    List<String> statuses = Stream.of(
      DataAccessEntityStatus.SUBMITTED,
      DataAccessEntityStatus.REVIEWED,
      DataAccessEntityStatus.APPROVED,
      DataAccessEntityStatus.REJECTED).map(DataAccessEntityStatus::name).collect(Collectors.toList());

    if (event.getConfig().isDaoCanEdit()) {
      getService().findByStatus(statuses).forEach(darEntity -> subjectAclService.addGroupPermission(Roles.MICA_DAO, getResourcePath(), "EDIT", darEntity.getId()));
    } else {
      getService().findByStatus(statuses).forEach(darEntity -> subjectAclService.removeGroupPermission(Roles.MICA_DAO, getResourcePath(), "EDIT", darEntity.getId()));
    }
  }

  //
  // Private methods
  //

  /**
   * Create or update a variables set from user's cart.
   *
   * @param entity
   * @return
   */
  protected DocumentSet createOrUpdateVariablesSet(DataAccessEntity entity) {
    DocumentSet set;
    DocumentSet cart = variableSetService.getCartCurrentUser();
    String setId = String.format("dar:%s", entity.getId());
    Optional<DocumentSet> setOpt = variableSetService.getAllCurrentUser().stream().filter(docset -> setId.equals(docset.getName())).findFirst();
    if (setOpt.isPresent()) {
      // reuse and append an existing set with same name
      set = variableSetService.addIdentifiers(setId, Lists.newArrayList(cart.getIdentifiers()));
    } else {
      // create a new one
      set = variableSetService.create(setId, Lists.newArrayList(cart.getIdentifiers()));
    }
    // case an administrator is by-passing the flow
    if (!DataAccessEntityStatus.OPENED.equals(entity.getStatus())) {
      variableSetService.setLock(set, true);
    }
    return set;
  }

  protected Response submit(String id) {
    T request = getService().findById(id);
    boolean fromOpened = request.getStatus() == DataAccessEntityStatus.OPENED;
    boolean fromConditionallyApproved = request.getStatus() == DataAccessEntityStatus.CONDITIONALLY_APPROVED;
    if (fromOpened && !subjectAclService.isCurrentUser(request.getApplicant()) && !SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN)) {
      // only applicant can submit an opened request
      throw new ForbiddenException();
    }
    getService().updateStatus(id, DataAccessEntityStatus.SUBMITTED);
    if (fromOpened || fromConditionallyApproved) {
      applyApplicantNotEditablePermissions(request.getApplicant(), id);
    }
    if (request.hasVariablesSet()) {
      DocumentSet set = request.getVariablesSet();
      variableSetService.setLock(set, true);
    }
    // link to latest form revision
    request = getService().findById(id);
    request.setFormRevision(getFormLatestRevision());
    getService().save(request);
    return Response.noContent().build();
  }

  protected Response open(String id) {
    T request = getService().updateStatus(id, DataAccessEntityStatus.OPENED);
    applyApplicantEditablePermissions(id, request.getApplicant());
    // set draft version
    request = getService().findById(id);
    request.setFormRevision(null);
    getService().save(request);
    return Response.noContent().build();
  }

  protected Response review(String id) {
    DataAccessEntity request = getService().findById(id);
    boolean fromConditionallyApproved = request.getStatus() == DataAccessEntityStatus.CONDITIONALLY_APPROVED;
    if (fromConditionallyApproved) {
      applyApplicantNotEditablePermissions(request.getApplicant(), id);
    }
    return updateStatus(id, DataAccessEntityStatus.REVIEWED);
  }

  protected Response approve(String id) {
    DataAccessEntity request = getService().findById(id);
    Response response = updateStatus(id, DataAccessEntityStatus.APPROVED);
    applyApplicantNotEditablePermissions(request.getApplicant(), id);
    return response;
  }

  protected Response reject(String id) {
    DataAccessEntity request = getService().findById(id);
    Response response = updateStatus(id, DataAccessEntityStatus.REJECTED);
    applyApplicantNotEditablePermissions(request.getApplicant(), id);
    return response;
  }

  protected Response conditionallyApprove(String id) {
    DataAccessEntity request = getService().findById(id);
    applyApplicantEditablePermissions(id, request.getApplicant());
    return updateStatus(id, DataAccessEntityStatus.CONDITIONALLY_APPROVED);
  }

  protected Response updateStatus(String id, DataAccessEntityStatus status) {
    getService().updateStatus(id, status);
    return Response.noContent().build();
  }

  Response doUpdateStatus(String id, String status) {
    subjectAclService.checkPermission(getResourcePath() + "/" + id, "EDIT", "_status");

    switch (DataAccessEntityStatus.valueOf(status.toUpperCase())) {
      case SUBMITTED:
        return submit(id);
      case OPENED:
        return open(id);
      case REVIEWED:
        return review(id);
      case CONDITIONALLY_APPROVED:
        return conditionallyApprove(id);
      case APPROVED:
        return approve(id);
      case REJECTED:
        return reject(id);
    }
    throw new BadRequestException("Unknown status");
  }

  /**
   * Permissions when form is in a state that is editable by the applicant.
   *
   * @param id
   * @param applicant
   */
  protected void applyApplicantEditablePermissions(String id, String applicant) {
    restoreApplicantActions(id, applicant);
    removeDAOActions(id);
  }

  protected void restoreApplicantActions(String id, String applicant) {
    // restore applicant permissions, i.e applicant cannot edit, nor delete request anymore + status cannot be changed
    subjectAclService.addUserPermission(applicant, getResourcePath(), "VIEW,EDIT,DELETE", id);
    subjectAclService.addUserPermission(applicant, getResourcePath() + "/" + id, "EDIT", "_status");
  }

  protected void removeDAOActions(String id) {
    // data access officers cannot change the status of this request anymore
    subjectAclService.removeGroupPermission(Roles.MICA_DAO, getResourcePath() + "/" + id, "EDIT", "_status");

    if (dataAccessConfigService.getOrCreateConfig().isDaoCanEdit()) {
      subjectAclService.removeGroupPermission(Roles.MICA_DAO, getResourcePath(), "EDIT", id);
    }
  }

  /**
   * Permissions when form is in a state that is not editable by the applicant.
   *
   * @param id
   */
  protected void applyApplicantNotEditablePermissions(String applicant, String id) {
    applyApplicantNotEditablePermissions(applicant, getResourcePath(), id);
  }

  protected void applyApplicantNotEditablePermissions(String applicant, String resourcePath, String id) {
    removeApplicantPermissions(applicant, resourcePath, id);
    restoreDaoActions(resourcePath, id);
  }

  private void restoreDaoActions(String resourcePath, String id) {
    // data access officers can change the status of this request
    subjectAclService.addGroupPermission(Roles.MICA_DAO, resourcePath + "/" + id, "EDIT", "_status");

    if (dataAccessConfigService.getOrCreateConfig().isDaoCanEdit()) {
      subjectAclService.addGroupPermission(Roles.MICA_DAO, resourcePath, "EDIT", id);
    }
  }

  private void removeApplicantPermissions(String applicant, String resourcePath, String id) {
    subjectAclService.removeUserPermission(applicant, resourcePath, "EDIT,DELETE", id);
    subjectAclService.removeUserPermission(applicant, resourcePath + "/" + id, "EDIT", "_status");
  }
}
