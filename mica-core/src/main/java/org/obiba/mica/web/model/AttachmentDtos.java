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


@Component
class AttachmentDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttributeDtos attributeDtos;

  @Inject
  private PermissionsDtos permissionsDtos;

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
  Mica.FileDto asFileDto(AttachmentState state, boolean publishedFileSystem, boolean detailed) {
    Mica.FileDto.Builder builder = asFileDto(state).toBuilder();

    if(publishedFileSystem) {
      builder.clearRevisionStatus();
      Attachment attachment = state.getAttachment();
      if (!Strings.isNullOrEmpty(attachment.getType()))
        builder.setMediaType(attachment.getType());
      if(attachment.getDescription() != null)
        builder.addAllDescription(localizedStringDtos.asDto(attachment.getDescription()));
    } else {
      builder.setState(asDto(state, detailed));
      builder.setPermissions(permissionsDtos.asDto(state));
    }

    if(builder.getType() == Mica.FileType.FOLDER) {
      // get the number of files in the folder
      builder.setSize(fileSystemService.countAttachmentStates(state.getPath(), publishedFileSystem));
    }

    return builder.build();
  }

  Mica.AttachmentStateDto asDto(AttachmentState state, boolean detailed) {
    Mica.AttachmentStateDto.Builder builder = Mica.AttachmentStateDto.newBuilder();
    builder.setId(state.getId()) //
      .setName(state.getName()) //
      .setPath(state.getPath()) //
      .setTimestamps(TimestampsDtos.asDto(state)) //
      .setAttachment(asDto(state.getAttachment()));
    if(state.isPublished()) {
      builder.setPublishedId(state.getPublishedAttachment().getId()) //
        .setPublicationDate(state.getPublicationDate().toString());

      if(state.getPublishedBy() != null) builder.setPublishedBy(state.getPublishedBy());
    }
    if(detailed && !FileUtils.isDirectory(state)) builder.addAllRevisions(
      fileSystemService.getAttachmentRevisions(state).stream().map(this::asDto).collect(Collectors.toList()));
    return builder.build();
  }

  @NotNull
  Mica.AttachmentDto asDto(@NotNull Attachment attachment) {
    Mica.AttachmentDto.Builder builder = Mica.AttachmentDto.newBuilder().setId(attachment.getId())
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
    if(attachment.hasLastModifiedBy()) builder.setLastModifiedBy(attachment.getLastModifiedBy().get());

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
    attachment.setSize((long)dto.getSize());
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
