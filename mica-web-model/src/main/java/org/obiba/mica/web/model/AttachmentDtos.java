package org.obiba.mica.web.model;

import java.util.Locale;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.service.FileSystemService;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import static org.obiba.mica.web.model.Mica.AttachmentDto;

@Component
class AttachmentDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttributeDtos attributeDtos;

  @Inject
  private FileSystemService fileSystemService;

  @NotNull
  Mica.FileDto asFileDto(AttachmentState state) {
    boolean isFolder = FileUtils.isDirectory(state);
    String name = state.getName();
    String path = String.format("%s/%s", state.getPath(), state.getName());
    if(isFolder) {
      int idx = state.getPath().lastIndexOf('/');
      name = state.getPath().substring(idx + 1);
      path = state.getPath();
    }
    Mica.FileDto.Builder builder = Mica.FileDto.newBuilder();
    return builder.setPath(path) //
      .setName(name) //
      .setTimestamps(TimestampsDtos.asDto(state)) //
      .setType(isFolder ? Mica.FileType.FOLDER : Mica.FileType.FILE) //
      .setSize(state.getAttachment().getSize()) //
      .setRevisionStatus(state.getRevisionStatus().name()) //
      .build();
  }

  @NotNull
  Mica.FileDto asFileDto(AttachmentState state, boolean publishedFileSystem) {
    Mica.FileDto.Builder builder = asFileDto(state).toBuilder();

    if(publishedFileSystem) {
      builder.clearRevisionStatus();
      if(state.isPublished()) {
        builder.setAttachment(asDto(state.getPublishedAttachment()));
      }
    } else builder.setState(asDto(state));

    if(builder.getType() == Mica.FileType.FOLDER) {
      // get the number of files recursively
      String pathRegEx = String.format("^%s", state.getPath());
      builder.setSize(fileSystemService.findAttachmentStates(pathRegEx, publishedFileSystem).stream()
        .filter(s -> !FileUtils.isDirectory(s)).collect(Collectors.toList()).size());
    }

    return builder.build();
  }

  Mica.AttachmentStateDto asDto(AttachmentState state) {
    Mica.AttachmentStateDto.Builder builder = Mica.AttachmentStateDto.newBuilder();
    builder.setId(state.getId()) //
      .setName(state.getName()) //
      .setPath(state.getPath()) //
      .setTimestamps(TimestampsDtos.asDto(state)) //
      .setAttachment(asDto(state.getAttachment()));
    if(state.isPublished()) {
      builder.setPublishedAttachment(asDto(state.getPublishedAttachment())) //
        .setPublicationDate(state.getPublicationDate().toString());
    }
    builder.addAllRevisions(
      fileSystemService.getAttachmentRevisions(state).stream().map(this::asDto).collect(Collectors.toList()));
    return builder.build();
  }

  @NotNull
  AttachmentDto asDto(@NotNull Attachment attachment) {
    AttachmentDto.Builder builder = AttachmentDto.newBuilder().setId(attachment.getId())
      .setFileName(attachment.getName()).setTimestamps(TimestampsDtos.asDto(attachment));
    if(attachment.getType() != null) builder.setType(attachment.getType());
    if(attachment.getDescription() != null) {
      builder.addAllDescription(localizedStringDtos.asDto(attachment.getDescription()));
    }
    if(attachment.getMd5() != null) {
      builder.setMd5(attachment.getMd5()).setSize(attachment.getSize());
      if(attachment.getLang() != null) builder.setLang(attachment.getLang().toString());
    }
    if(attachment.getAttributes() != null) {
      attachment.getAttributes().asAttributeList()
        .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }
    if(!Strings.isNullOrEmpty(attachment.getPath())) builder.setPath(attachment.getPath());

    return builder.build();
  }

  @NotNull
  Attachment fromDto(@NotNull Mica.AttachmentDtoOrBuilder dto) {
    Attachment attachment = new Attachment();
    attachment.setId(dto.getId());
    attachment.setName(dto.getFileName());
    if(dto.hasType()) attachment.setType(dto.getType());
    if(dto.getDescriptionCount() > 0) attachment.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    if(dto.hasLang()) attachment.setLang(new Locale(dto.getLang()));
    attachment.setSize(dto.getSize());
    if(dto.hasMd5()) attachment.setMd5(dto.getMd5());
    attachment.setJustUploaded(dto.getJustUploaded());

    if(dto.hasTimestamps()) TimestampsDtos.fromDto(dto.getTimestamps(), attachment);

    if(dto.getAttributesCount() > 0) {
      dto.getAttributesList().forEach(attributeDto -> attachment.addAttribute(attributeDtos.fromDto(attributeDto)));
    }

    if(dto.hasPath()) attachment.setPath(dto.getPath());

    return attachment;
  }
}
