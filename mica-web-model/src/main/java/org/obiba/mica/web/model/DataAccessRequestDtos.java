package org.obiba.mica.web.model;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.file.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class DataAccessRequestDtos {

  private static final Logger log = LoggerFactory.getLogger(DataAccessRequestDtos.class);


  @Inject
  private AttachmentDtos attachmentDtos;

  @NotNull
  public Mica.DataAccessRequestDto asDto(@NotNull DataAccessRequest request) {
    Mica.DataAccessRequestDto.Builder builder = Mica.DataAccessRequestDto.newBuilder();
    builder.setApplicant(request.getApplicant()) //
      .setStatus(request.getStatus().name()) //
      .setTitle(request.getTitle());
    if(request.hasContent()) builder.setContent(request.getContent());
    if(!request.isNew()) builder.setId(request.getId());

    request.getAttachments().forEach(attachment -> builder.addAttachments(attachmentDtos.asDto(attachment)));

    return builder.build();
  }

  @NotNull
  public DataAccessRequest fromDto(@NotNull Mica.DataAccessRequestDto dto) {
    DataAccessRequest.Builder builder = DataAccessRequest.newBuilder();
    builder.applicant(dto.getApplicant()).title(dto.getTitle()).status(dto.getStatus());
    if(dto.hasContent()) builder.content(dto.getContent());

    DataAccessRequest request = builder.build();
    if (dto.hasId()) request.setId(dto.getId());

    if(dto.getAttachmentsCount() > 0) {
      request.setAttachments(
        dto.getAttachmentsList().stream().map(attachmentDtos::fromDto).collect(Collectors.<Attachment>toList()));
    }

    return request;
  }
}
