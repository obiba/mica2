package org.obiba.mica.access.service;

import com.google.common.eventbus.Subscribe;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessEntityWithParent;
import org.obiba.mica.micaConfig.event.DataAccessConfigUpdatedEvent;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Grants or revokes the DAO group's EDIT permission on existing data access entities when "DAO can edit" changes.
 * Lives in a singleton service so that it runs on the event bus thread, without a Shiro subject.
 */
@Service
public class DataAccessDaoPermissionService {

  private static final List<String> STATUSES = Stream.of(
    DataAccessEntityStatus.SUBMITTED,
    DataAccessEntityStatus.REVIEWED,
    DataAccessEntityStatus.APPROVED,
    DataAccessEntityStatus.REJECTED).map(DataAccessEntityStatus::name).toList();

  private final SubjectAclService subjectAclService;

  // entity service -> ACL resource path of an entity
  private final Map<DataAccessEntityService<?>, Function<DataAccessEntity, String>> resourcePaths;

  @Inject
  public DataAccessDaoPermissionService(
    SubjectAclService subjectAclService,
    DataAccessRequestService dataAccessRequestService,
    DataAccessFeasibilityService dataAccessFeasibilityService,
    DataAccessAmendmentService dataAccessAmendmentService,
    DataAccessPreliminaryService dataAccessPreliminaryService,
    DataAccessAgreementService dataAccessAgreementService) {
    this.subjectAclService = subjectAclService;
    this.resourcePaths = Map.of(
      dataAccessRequestService, entity -> "/data-access-request",
      dataAccessFeasibilityService, entity -> childPath(entity, "feasibility"),
      dataAccessAmendmentService, entity -> childPath(entity, "amendment"),
      dataAccessPreliminaryService, entity -> childPath(entity, "preliminary"),
      dataAccessAgreementService, entity -> childPath(entity, "agreement"));
  }

  @Subscribe
  public void onDataAccessConfigUpdated(DataAccessConfigUpdatedEvent event) {
    boolean daoCanEdit = event.getConfig().isDaoCanEdit();
    resourcePaths.forEach((service, resourcePath) -> service.findByStatus(STATUSES).forEach(entity -> {
      if (daoCanEdit)
        subjectAclService.addGroupPermission(Roles.MICA_DAO, resourcePath.apply(entity), "EDIT", entity.getId());
      else
        subjectAclService.removeGroupPermission(Roles.MICA_DAO, resourcePath.apply(entity), "EDIT", entity.getId());
    }));
  }

  private static String childPath(DataAccessEntity entity, String type) {
    return String.format("/data-access-request/%s/%s", ((DataAccessEntityWithParent) entity).getParentId(), type);
  }
}
