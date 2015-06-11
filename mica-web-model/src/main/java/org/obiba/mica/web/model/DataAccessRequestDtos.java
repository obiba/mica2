package org.obiba.mica.web.model;

import java.nio.file.Paths;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.file.Attachment;
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
  private DataAccessRequestUtilService dataAccessRequestUtilService;

  @NotNull
  public Mica.DataAccessRequestDto asDto(@NotNull DataAccessRequest request) {
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

    request.getAttachments().forEach(attachment -> builder.addAttachments(attachmentDtos.asDto(attachment)));

    request.getStatusChangeHistory()
      .forEach(statusChange -> builder.addStatusChangeHistory(statusChangeDtos.asDto(statusChange)));

    // possible actions depending on the caller
    if(subjectAclService.isPermitted("/data-access-request", "VIEW", request.getId())) {
      builder.addActions("VIEW");
    }
    if(subjectAclService.isPermitted("/data-access-request", "EDIT", request.getId())) {
      builder.addActions("EDIT");
    }
    if(subjectAclService.isPermitted("/data-access-request", "DELETE", request.getId())) {
      builder.addActions("DELETE");
    }
    if(subjectAclService
      .isPermitted(Paths.get("/data-access-request", request.getId()).toString(), "EDIT", "_status")) {
      builder.addActions("EDIT_STATUS");
    }

    ObibaRealm.Subject profile = userProfileService.getProfile(request.getApplicant());
    if(profile != null) {
      builder.setProfile(userProfileDtos.asDto(profile));
    }

    // possible status transitions
    dataAccessRequestUtilService.nextStatus(request).forEach(status -> builder.addNextStatus(status.toString()));

    return builder.build();
  }

  @NotNull
  public DataAccessRequest fromDto(@NotNull Mica.DataAccessRequestDto dto) {
    DataAccessRequest.Builder builder = DataAccessRequest.newBuilder();
    builder.applicant(dto.getApplicant()).status(dto.getStatus());
    if(dto.hasContent()) builder.content(dto.getContent());

    DataAccessRequest request = builder.build();
    if(dto.hasId()) request.setId(dto.getId());

    if(dto.getAttachmentsCount() > 0) {
      request.setAttachments(
        dto.getAttachmentsList().stream().map(attachmentDtos::fromDto).collect(Collectors.<Attachment>toList()));
    }
    TimestampsDtos.fromDto(dto.getTimestamps(), request);

    return request;
  }
}
