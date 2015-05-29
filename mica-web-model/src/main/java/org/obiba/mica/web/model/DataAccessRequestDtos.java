package org.obiba.mica.web.model;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.stereotype.Component;

@Component
class DataAccessRequestDtos {

  @Inject
  private AttachmentDtos attachmentDtos;

  @Inject
  private SubjectAclService subjectAclService;

  @NotNull
  public Mica.DataAccessRequestDto asDto(@NotNull DataAccessRequest request) {
    Mica.DataAccessRequestDto.Builder builder = Mica.DataAccessRequestDto.newBuilder();
    builder.setApplicant(request.getApplicant()) //
      .setStatus(request.getStatus().name()) //
      .setTitle(request.getTitle()) //
      .setTimestamps(TimestampsDtos.asDto(request));
    if(request.hasContent()) builder.setContent(request.getContent());
    if(!request.isNew()) builder.setId(request.getId());

    request.getAttachments().forEach(attachment -> builder.addAttachments(attachmentDtos.asDto(attachment)));

    // possible actions depending on the caller
    if (subjectAclService.isPermitted("/data-access-request", "VIEW", request.getId())) {
      builder.addActions("VIEW");
    }
    if (subjectAclService.isPermitted("/data-access-request", "EDIT", request.getId())) {
      builder.addActions("EDIT");
    }
    if (subjectAclService.isPermitted("/data-access-request", "DELETE", request.getId())) {
      builder.addActions("DELETE");
    }
    if (subjectAclService.isPermitted("/data-access-request/" + request.getId(), "EDIT", "_status")) {
      builder.addActions("EDIT_STATUS");
    }

    // possible status transitions
    request.nextStatus().forEach(status -> builder.addNextStatus(status.toString()));

    return builder.build();
  }

  @NotNull
  public DataAccessRequest fromDto(@NotNull Mica.DataAccessRequestDto dto) {
    DataAccessRequest.Builder builder = DataAccessRequest.newBuilder();
    builder.applicant(dto.getApplicant()).title(dto.getTitle()).status(dto.getStatus());
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
