/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.obiba.mica.access.DataAccessRequestRepository;
import org.obiba.mica.access.domain.*;
import org.obiba.mica.access.notification.DataAccessRequestReportNotificationService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.service.NoSuchProjectException;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.model.Mica.DataAccessRequestDto.StatusChangeDto.Builder;
import org.obiba.shiro.realm.ObibaRealm.Subject;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Component
class DataAccessRequestDtos {

  private static final SimpleDateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd");

  @Inject
  private AttachmentDtos attachmentDtos;

  @Inject
  private StatusChangeDtos statusChangeDtos;

  @Inject
  private ActionLogDtos actionLogDtos;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private UserProfileService userProfileService;

  @Inject
  private UserProfileDtos userProfileDtos;

  @Inject
  private PermissionsDtos permissionsDtos;

  @Inject
  private DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  private DataAccessRequestRepository dataAccessRequestRepository;

  @Inject
  private ProjectService projectService;

  @Inject
  private DataAccessRequestReportNotificationService dataAccessRequestReportNotificationService;

  public StatusChangeDtos getStatusChangeDtos() {
    return statusChangeDtos;
  }

  Mica.DataAccessRequestDto.Builder asDtoBuilder(@NotNull DataAccessEntity request) {
    Mica.DataAccessRequestDto.Builder builder = asMinimalistDtoBuilder(request, true);

    boolean canAccessActionLogs = SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) ||
      SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN) ||
      subjectAclService.isPermitted("/data-access-request/action-logs", "VIEW");

    if (canAccessActionLogs) {
      request.getActionLogHistory()
        .forEach(actionLog -> builder.addActionLogHistory(actionLogDtos.asDto(actionLog)));
    }

    Map<String, Subject> micaProfiles = userProfileService.getProfilesByGroup(null).stream().collect(Collectors.toMap(Subject::getUsername, profile -> profile));

    if (micaProfiles.containsKey(request.getApplicant())) {
      builder.setProfile(userProfileDtos.asDto(micaProfiles.get(request.getApplicant())));
    }

    builder.addAllStatusChangeHistory(asStatusChangeDtoList(request, micaProfiles));

    // possible status transitions
    dataAccessRequestUtilService.nextStatus(request).forEach(status -> builder.addNextStatus(status.toString()));

    if (request.hasFormRevision())
      builder.setFormRevision(request.getFormRevision());

    return builder;
  }

  @NotNull
  public Mica.DataAccessRequestDto asDto(@NotNull DataAccessRequest request) {
    Mica.DataAccessRequestDto.Builder builder = asDtoBuilder(request);

    String title = dataAccessRequestUtilService.getRequestTitle(request);
    if (!Strings.isNullOrEmpty(title)) {
      builder.setTitle(title);
    }

    if (request.hasStartDate()) builder.setStartDate(ISO_8601.format(request.getStartDate()));

    DataAccessRequestTimeline timeline = dataAccessRequestReportNotificationService.getReportsTimeline(request);
    if (timeline.hasStartDate() && timeline.hasEndDate()) {
      builder.setReportsTimeline(Mica.TimelineDto.newBuilder()
        .setStartDate(ISO_8601.format(timeline.getStartDate()))
        .setEndDate(ISO_8601.format(timeline.getEndDate()))
        .addAllIntermediateDates(timeline.getIntermediateDates().stream().map(d -> ISO_8601.format(d)).collect(Collectors.toList())));
    }

    builder.setArchived(request.isArchived());

    request.getAttachments().forEach(attachment -> builder.addAttachments(attachmentDtos.asDto(attachment)));

    if (!request.isArchived()) {
      builder.addAllActions(addDataAccessEntityActions(request, "/data-access-request"));

      if (subjectAclService
        .isPermitted(Paths.get("/data-access-request", request.getId(), "/amendment").toString(), "ADD")) {
        builder.addActions("ADD_AMENDMENTS");
      }

      boolean hasAdministrativeRole = SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) || SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN);

      if (hasAdministrativeRole || subjectAclService.isPermitted("/data-access-request/private-comment", "VIEW")) {
        builder.addActions("VIEW_PRIVATE_COMMENTS");
      }

      if (hasAdministrativeRole || subjectAclService.isPermitted(Paths.get("/data-access-request", request.getId(), "_attachments").toString(), "EDIT")) {
        builder.addActions("EDIT_ATTACHMENTS");

        if (hasAdministrativeRole) {
          builder.addActions("DELETE_ATTACHMENTS");
          builder.addActions("EDIT_ACTION_LOGS");
        }
      }
    }

    try {
      Project project = projectService.findById(request.getId());
      Mica.PermissionsDto permissionsDto = permissionsDtos.asDto(project);

      Mica.ProjectSummaryDto.Builder projectSummaryDtoBuilder = Mica.ProjectSummaryDto.newBuilder();
      projectSummaryDtoBuilder.setId(project.getId());
      projectSummaryDtoBuilder.setPermissions(permissionsDto);
      builder.setProject(projectSummaryDtoBuilder.build());
    } catch (NoSuchProjectException e) {
      // do nothing
    }

    return builder.build();
  }

  void fromDto(@NotNull Mica.DataAccessRequestDto dto, DataAccessEntity.Builder builder) {
    builder.applicant(dto.getApplicant()).status(dto.getStatus());
    if (dto.hasContent()) builder.content(dto.getContent());
  }

  @NotNull
  public DataAccessRequest fromDto(@NotNull Mica.DataAccessRequestDto dto) {
    DataAccessRequest.Builder builder = DataAccessRequest.newBuilder();
    fromDto(dto, builder);
    DataAccessRequest request = (DataAccessRequest) builder.build();
    if (dto.hasId()) request.setId(dto.getId());

    if (dto.hasStartDate()) {
      try {
        request.setStartDate(ISO_8601.parse(dto.getStartDate()));
      } catch (ParseException e) {
        // ignore
      }
    }

    request.setArchived(dto.getArchived());

    if (dto.getActionLogHistoryCount() > 0) {
      request.setActionLogHistory(
        dto.getActionLogHistoryList().stream().map(actionLogDtos::fromDto).collect(Collectors.toList()));
    }

    if (dto.getAttachmentsCount() > 0) {
      request.setAttachments(
        dto.getAttachmentsList().stream().map(attachmentDtos::fromDto).collect(Collectors.toList()));
    }
    TimestampsDtos.fromDto(dto.getTimestamps(), request);
    return (DataAccessRequest) builder.build();
  }

  @NotNull
  public Mica.DataAccessRequestDto asPreliminaryDto(@NotNull DataAccessPreliminary preliminary) {
    return asDtoBuilder(preliminary).build();
  }

  @NotNull
  public Mica.DataAccessRequestDto asFeasibilityDto(@NotNull DataAccessFeasibility feasibility) {
    return asDtoBuilder(feasibility).build();
  }

  @NotNull
  public DataAccessPreliminary fromPreliminaryDto(@NotNull Mica.DataAccessRequestDto dto) {
    Assert.isTrue(Mica.DataAccessRequestDto.Type.PRELIMINARY == dto.getType(), "Expected preliminary request type");
    DataAccessPreliminary.Builder builder = DataAccessPreliminary.newBuilder();
    builder.parentId(dto.getParentId());

    fromDto(dto, builder);
    DataAccessPreliminary preliminary = (DataAccessPreliminary) builder.build();
    if (dto.hasId()) preliminary.setId(dto.getId());
    TimestampsDtos.fromDto(dto.getTimestamps(), preliminary);

    return (DataAccessPreliminary) builder.build();
  }

  @NotNull
  public DataAccessFeasibility fromFeasibilityDto(@NotNull Mica.DataAccessRequestDto dto) {
    Assert.isTrue(Mica.DataAccessRequestDto.Type.FEASIBILITY == dto.getType(), "Expected feasibility request type");
    DataAccessFeasibility.Builder builder = DataAccessFeasibility.newBuilder();
    builder.parentId(dto.getParentId());

    fromDto(dto, builder);
    DataAccessFeasibility feasibility = (DataAccessFeasibility) builder.build();
    if (dto.hasId()) feasibility.setId(dto.getId());
    TimestampsDtos.fromDto(dto.getTimestamps(), feasibility);

    return (DataAccessFeasibility) builder.build();
  }

  @NotNull
  public Mica.DataAccessRequestDto asAgreementDto(@NotNull DataAccessAgreement agreement) {
    return asDtoBuilder(agreement).build();
  }

  @NotNull
  public DataAccessAgreement fromAgreementDto(@NotNull Mica.DataAccessRequestDto dto) {
    Assert.isTrue(Mica.DataAccessRequestDto.Type.AGREEMENT == dto.getType(), "Expected agreement request type");
    DataAccessFeasibility.Builder builder = DataAccessFeasibility.newBuilder();
    builder.parentId(dto.getParentId());

    fromDto(dto, builder);
    DataAccessAgreement agreement = (DataAccessAgreement) builder.build();
    if (dto.hasId()) agreement.setId(dto.getId());
    TimestampsDtos.fromDto(dto.getTimestamps(), agreement);

    return (DataAccessAgreement) builder.build();
  }

  @NotNull
  public Mica.DataAccessRequestDto asAmendmentDto(@NotNull DataAccessAmendment amendment) {
    return asDtoBuilder(amendment).build();
  }

  @NotNull
  public DataAccessAmendment fromAmendmentDto(@NotNull Mica.DataAccessRequestDto dto) {
    Assert.isTrue(Mica.DataAccessRequestDto.Type.AMENDMENT == dto.getType(), "Expected amendment request type");
    DataAccessAmendment.Builder builder = DataAccessAmendment.newBuilder();
    builder.parentId(dto.getParentId());

    fromDto(dto, builder);
    DataAccessAmendment amendment = (DataAccessAmendment) builder.build();
    if (dto.hasId()) amendment.setId(dto.getId());
    TimestampsDtos.fromDto(dto.getTimestamps(), amendment);

    return (DataAccessAmendment) builder.build();
  }
  @NotNull
  public Mica.DataAccessCollaboratorDto asDto(@NotNull DataAccessCollaborator collaborator) {
    Mica.DataAccessCollaboratorDto.Builder builder = Mica.DataAccessCollaboratorDto.newBuilder();

    if (collaborator.hasLastModifiedBy()) {
      builder.setModifiedBy(collaborator.getLastModifiedBy().get());
    }

    return builder
      .setRequestId(collaborator.getRequestId())
      .setEmail(collaborator.getEmail())
      .setInvitationPending(collaborator.isInvitationPending())
      .setCreatedBy(collaborator.getCreatedBy().get())
      .setTimestamps(TimestampsDtos.asDto(collaborator))
      .build();
  }

  @NotNull
  public ActionLog fromDto(Mica.DataAccessRequestDto.ActionLogDto dto) {
    return actionLogDtos.fromDto(dto);
  }

  @NotNull
  List<Mica.DataAccessRequestDto.StatusChangeDto> asStatusChangeDtoList(@NotNull DataAccessEntity entity) {
    Map<String, Subject> micaProfiles = userProfileService.getProfilesByGroup(null).stream().collect(Collectors.toMap(Subject::getUsername, profile -> profile));
    return asStatusChangeDtoList(entity, micaProfiles);
  }

  @NotNull
  List<Mica.DataAccessRequestDto.StatusChangeDto> asStatusChangeDtoList(@NotNull DataAccessEntity entity, Map<String, Subject> micaProfiles) {
    return entity.getStatusChangeHistory().stream().map(statusChange -> {
      Builder builder = statusChangeDtos.asMinimalistDtoBuilder(statusChange);

      if (entity instanceof DataAccessAmendment) {
        builder.setReference(entity.getId());
      }

      if (micaProfiles.containsKey(statusChange.getAuthor())) {
        builder.setProfile(userProfileDtos.asDto(micaProfiles.get(statusChange.getAuthor())));
      }

      return builder.build();
    }).collect(Collectors.toList());
  }

  List<Mica.DataAccessRequestDto> asDtoList(@NotNull List<DataAccessRequest> requests) {
    if (requests != null) {

      Map<String, Subject> micaProfiles = userProfileService.getProfilesByGroup(null).stream().collect(Collectors.toMap(Subject::getUsername, profile -> profile));
      Map<Object, LinkedHashMap> allAmendmentsSummary = dataAccessRequestRepository.getAllAmendmentsSummary();

      return requests.stream()
        .map(request -> asMinimalistDtoBuilder(request, false))
        .map(builder -> setUserProfileAndAmendmentsSummary(builder, micaProfiles, allAmendmentsSummary).build()).collect(Collectors.toList());
    }

    return new ArrayList<>();
  }

  private Mica.DataAccessRequestDto.Builder asMinimalistDtoBuilder(DataAccessEntity dataAccessEntity, boolean omitStatusChangeHistory) {
    Mica.DataAccessRequestDto.Builder builder = Mica.DataAccessRequestDto.newBuilder();

    builder
      .setApplicant(dataAccessEntity.getApplicant())
      .setStatus(dataAccessEntity.getStatus().name())
      .setTimestamps(TimestampsDtos.asDto(dataAccessEntity));

    if (dataAccessEntity instanceof DataAccessRequest)
      builder.setArchived(((DataAccessRequest)dataAccessEntity).isArchived());

    if (dataAccessEntity.hasContent()) builder.setContent(dataAccessEntity.getContent());
    if (!dataAccessEntity.isNew()) builder.setId(dataAccessEntity.getId());

    String title = dataAccessRequestUtilService.getRequestTitle(dataAccessEntity);
    if (!Strings.isNullOrEmpty(title)) builder.setTitle(title);

    if (!omitStatusChangeHistory)
      dataAccessEntity.getStatusChangeHistory().forEach(statusChange -> builder.addStatusChangeHistory(statusChangeDtos.asMinimalistDtoBuilder(statusChange)));

    setMinimalistActions(builder, dataAccessEntity);

    return builder;
  }

  private void setMinimalistActions(Mica.DataAccessRequestDto.Builder builder, DataAccessEntity dataAccessEntity) {
    if (dataAccessEntity instanceof DataAccessAmendment) {

      DataAccessAmendment amendment = (DataAccessAmendment) dataAccessEntity;
      builder.setType(Mica.DataAccessRequestDto.Type.AGREEMENT);
      builder.setParentId(amendment.getParentId());

      builder.addAllActions(
        addDataAccessEntityActions(
          amendment,
          String.format("/data-access-request/%s/amendment", amendment.getParentId())
        )
      );
    } else {
      builder.addAllActions(addDataAccessEntityActions(dataAccessEntity, "/data-access-request"));
    }
  }

  private void setAmendmentsSummary(Mica.DataAccessRequestDto.Builder builder, Map<Object, LinkedHashMap> pendingAmendments) {
    if (!pendingAmendments.isEmpty() && builder.hasId() && pendingAmendments.containsKey(builder.getId())) {

      LinkedHashMap map = pendingAmendments.get(builder.getId());

      builder.setAmendmentsSummary(
        Mica.DataAccessRequestDto.AmendmentsSummaryDto.newBuilder()
          .setId(map.get("_id") + "")
          .setPending((int)map.get("pending"))
          .setTotal((int)map.get("total"))
          .setLastModifiedDate(map.get("lastModified") instanceof Date ? ((Date) map.get("lastModified")).toInstant().atOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now())).toString() : "")
          .build()
      );
    }
  }

  private Mica.DataAccessRequestDto.Builder setUserProfileAndAmendmentsSummary(Mica.DataAccessRequestDto.Builder dataAccessEntityBuilder,
                                                                               Map<String, Subject> micaProfiles,
                                                                               Map<Object, LinkedHashMap> pendingAmendments) {
    if (micaProfiles != null && dataAccessEntityBuilder.hasApplicant() && micaProfiles.containsKey(dataAccessEntityBuilder.getApplicant())) {
      dataAccessEntityBuilder.setProfile(userProfileDtos.asDto(micaProfiles.get(dataAccessEntityBuilder.getApplicant())));
    }

    setAmendmentsSummary(dataAccessEntityBuilder, pendingAmendments);

    return dataAccessEntityBuilder;
  }

  private List<String> addDataAccessEntityActions(DataAccessEntity entity, String resource) {
    List<String> actions = Lists.newArrayList();

    if (entity instanceof DataAccessAmendment && subjectAclService.isPermitted("/data-access-request", "VIEW", ((DataAccessAmendment) entity).getParentId())) {
      actions.add("VIEW");
    }

    if (entity instanceof DataAccessRequest && subjectAclService.isPermitted(resource, "VIEW", entity.getId())) {
      actions.add("VIEW");
    }

    if (subjectAclService.isPermitted(resource, "EDIT", entity.getId())) {
      actions.add("EDIT");
    }

    if (subjectAclService.isPermitted(resource, "DELETE", entity.getId())) {
      actions.add("DELETE");
    }

    if (subjectAclService.isPermitted(Paths.get(resource, entity.getId()).toString(), "EDIT", "_status")) {
      actions.add("EDIT_STATUS");
    }

    return actions;
  }
}
