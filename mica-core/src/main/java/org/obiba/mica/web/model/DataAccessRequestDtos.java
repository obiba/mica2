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

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.service.NoSuchProjectException;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.shiro.realm.ObibaRealm;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
class DataAccessRequestDtos {

  @Inject
  private AttachmentDtos attachmentDtos;

  @Inject
  private StatusChangeDtos statusChangeDtos;

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
  private ProjectService projectService;

  Mica.DataAccessRequestDto.Builder asDtoBuilder(@NotNull DataAccessEntity request) {
    Mica.DataAccessRequestDto.Builder builder = Mica.DataAccessRequestDto.newBuilder();
    builder.setApplicant(request.getApplicant()) //
      .setStatus(request.getStatus().name()) //
      .setTimestamps(TimestampsDtos.asDto(request)); //
    if(request.hasContent()) builder.setContent(request.getContent()); //
    if(!request.isNew()) builder.setId(request.getId());

    String title = dataAccessRequestUtilService.getRequestTitle(request);
    if(!Strings.isNullOrEmpty(title)) {
      builder.setTitle(title);
    }

    request.getStatusChangeHistory()
      .forEach(statusChange -> builder.addStatusChangeHistory(statusChangeDtos.asDto(statusChange)));

    ObibaRealm.Subject profile = userProfileService.getProfile(request.getApplicant());
    if(profile != null) {
      builder.setProfile(userProfileDtos.asDto(profile));
    }

    // possible status transitions
    dataAccessRequestUtilService.nextStatus(request).forEach(status -> builder.addNextStatus(status.toString()));

    return builder;
  }

  @NotNull
  public Mica.DataAccessRequestDto asDto(@NotNull DataAccessRequest request) {
    Mica.DataAccessRequestDto.Builder builder = asDtoBuilder(request);

    String title = dataAccessRequestUtilService.getRequestTitle(request);
    if(!Strings.isNullOrEmpty(title)) {
      builder.setTitle(title);
    }

    request.getAttachments().forEach(attachment -> builder.addAttachments(attachmentDtos.asDto(attachment)));

    builder.addAllActions(addDataAccessEntityActions(request, "/data-access-request"));

    if(subjectAclService
      .isPermitted(Paths.get("/data-access-request", request.getId(), "/amendment").toString(), "ADD")) {
      builder.addActions("ADD_AMENDMENTS");
    }

    boolean canDeleteAttachments = SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) || SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN);
    if (canDeleteAttachments ||
      subjectAclService.isPermitted(Paths.get("/data-access-request", request.getId(), "_attachments").toString(), "EDIT")) {
      if (canDeleteAttachments) {
        builder.addActions("DELETE_ATTACHMENTS");
      }

      builder.addActions("EDIT_ATTACHMENTS");
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
    if(dto.hasContent()) builder.content(dto.getContent());
  }

  @NotNull
  public DataAccessRequest fromDto(@NotNull Mica.DataAccessRequestDto dto) {
    DataAccessRequest.Builder builder = DataAccessRequest.newBuilder();
    fromDto(dto, builder);
    DataAccessRequest request = (DataAccessRequest)builder.build();
    if(dto.hasId()) request.setId(dto.getId());

    if(dto.getAttachmentsCount() > 0) {
      request.setAttachments(
        dto.getAttachmentsList().stream().map(attachmentDtos::fromDto).collect(Collectors.toList()));
    }
    TimestampsDtos.fromDto(dto.getTimestamps(), request);
    return (DataAccessRequest)builder.build();
  }

  @NotNull
  public Mica.DataAccessRequestDto asAmendmentDto(@NotNull DataAccessAmendment amendment) {
    Mica.DataAccessRequestDto.Builder builder = asDtoBuilder(amendment);
    builder.setExtension(
      Mica.DataAccessAmendmentDto.amendment,
      Mica.DataAccessAmendmentDto.newBuilder().setParentId(amendment.getParentId()).build()
    );

    if (subjectAclService.isPermitted("/data-access-request", "VIEW", amendment.getParentId())) {
      builder.addActions("VIEW");
    }

    builder.addAllActions(
      addDataAccessEntityActions(
        amendment,
        String.format("/data-access-request/%s/amendment", amendment.getParentId())
      )
    );

    return builder.build();
  }

  @NotNull
  public DataAccessAmendment fromAmendmentDto(@NotNull Mica.DataAccessRequestDto dto) {
    DataAccessAmendment.Builder builder = DataAccessAmendment.newBuilder();
    Mica.DataAccessAmendmentDto extension = dto.getExtension(Mica.DataAccessAmendmentDto.amendment);
    builder.parentId(extension.getParentId());

    fromDto(dto, builder);
    DataAccessAmendment amendment = (DataAccessAmendment)builder.build();
    if(dto.hasId()) amendment.setId(dto.getId());
    TimestampsDtos.fromDto(dto.getTimestamps(), amendment);

    return (DataAccessAmendment)builder.build();
  }

  private List<String> addDataAccessEntityActions(DataAccessEntity entity, String resource) {
    List<String> actions = Lists.newArrayList();

    // possible actions depending on the caller
    if(entity instanceof DataAccessRequest && subjectAclService.isPermitted(resource, "VIEW", entity.getId())) {
      actions.add("VIEW");
    }
    if(subjectAclService.isPermitted(resource, "EDIT", entity.getId())) {
      actions.add("EDIT");
    }
    if(subjectAclService.isPermitted(resource, "DELETE", entity.getId())) {
      actions.add("DELETE");
    }
    if(subjectAclService
      .isPermitted(Paths.get(resource, entity.getId()).toString(), "EDIT", "_status")) {
      actions.add("EDIT_STATUS");
    }
    return actions;
  }
}
