package org.obiba.mica.web.model;

import java.util.Locale;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Attachment;

import static org.obiba.mica.web.model.Mica.AttachmentDto;

class AttachmentDtos {

  private AttachmentDtos() {}

  @NotNull
  static AttachmentDto asDto(@NotNull Attachment attachment) {
    AttachmentDto.Builder builder = AttachmentDto.newBuilder().setFileName(attachment.getName());
    if (attachment.getType() != null) builder.setType(attachment.getType());
    if(attachment.getDescription() != null)
      builder.addAllDescription(LocalizedStringDtos.asDto(attachment.getDescription()));
    if (attachment.getLang() != null) builder.setLang(attachment.getLang().toString());
    if (attachment.getSize() != null) builder.setSize(attachment.getSize());
    if (attachment.getMd5() != null) builder.setMd5(attachment.getMd5());

    return builder.build();
  }

  @NotNull
  static Attachment fromDto(@NotNull AttachmentDto dto) {
    Attachment attachment = new Attachment();
    attachment.setName(dto.getFileName());
    if (dto.hasType()) attachment.setType(dto.getType());
    if(dto.getDescriptionCount() > 0) attachment.setDescription(LocalizedStringDtos.fromDto(dto.getDescriptionList()));
    if (dto.hasLang()) attachment.setLang(new Locale(dto.getLang()));
    if (dto.hasSize()) attachment.setSize(dto.getSize());
    if (dto.hasMd5()) attachment.setMd5(dto.getMd5());

    return attachment;
  }
}
