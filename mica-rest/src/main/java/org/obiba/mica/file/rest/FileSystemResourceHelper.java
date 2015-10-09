package org.obiba.mica.file.rest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import javafx.util.Pair;

@Component
@Scope("request")
public class FileSystemResourceHelper {

  @Inject
  protected FileSystemService fileSystemService;

  @Inject
  private Dtos dtos;

  private boolean published;

  public void setPublished(boolean published) {
    this.published = published;
  }

  public Mica.FileDto getFile(String path) {
    String basePath = normalizePath(path);
    if(isRoot(basePath)) return getFolderDto(basePath);
    if(basePath.endsWith("/")) return getFolderDto(basePath.replaceAll("[/]+$", ""));

    try {
      return getFileDto(basePath);
    } catch(NoSuchEntityException ex) {
      return getFolderDto(basePath);
    }
  }

  public void deleteFile(String path) {
    String basePath = normalizePath(path);
    if(isRoot(basePath)) deleteFolderState(basePath);
    if(basePath.endsWith("/")) deleteFolderState(basePath.replaceAll("[/]+$", ""));

    try {
      deleteFileState(basePath);
    } catch(NoSuchEntityException ex) {
      deleteFolderState(basePath);
    }
  }

  //
  // Private methods
  //

  private void deleteFileState(String basePath) {
    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);
    fileSystemService.delete(pathName.getKey(), pathName.getValue());
  }

  private void deleteFolderState(String basePath) {
    fileSystemService.delete(basePath);
  }

  private Mica.FileDto getFileDto(String basePath) {
    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);
    AttachmentState state = fileSystemService.getAttachmentState(pathName.getKey(), pathName.getValue(), published);
    return dtos.asFileDto(state);
  }

  private Mica.FileDto getFolderDto(String basePath) {
    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);

    Mica.FileDto.Builder builder = Mica.FileDto.newBuilder();
    builder.addAllChildren(getChildrenFolders(basePath)) //
      .addAllChildren(getChildrenFiles(basePath));

    if(builder.getChildrenCount() == 0) {
      if(isRoot(basePath)) {
        return builder.setPath(basePath) //
          .setName("") //
          .setType(Mica.FileType.FOLDER) //
          .setSize(0).build();
      }
      throw new NoSuchElementException("No file found at path: " + basePath);
    }

    builder.setPath(basePath) //
      .setName(pathName.getValue()) //
      .setType(Mica.FileType.FOLDER) //
      .setSize(
        builder.getChildrenList().stream().mapToLong(f -> f.getType() == Mica.FileType.FOLDER ? f.getSize() : 1).sum());

    return builder.build();
  }

  private Iterable<Mica.FileDto> getChildrenFolders(String basePath) {
    List<Mica.FileDto> folders = Lists.newArrayList();
    String pathRegEx = isRoot(basePath) ? "^/" : String.format("^%s/", basePath);
    fileSystemService.findAttachmentStates(pathRegEx, published).stream() //
      .collect(Collectors.groupingBy(new Function<AttachmentState, String>() {
        @Override
        public String apply(AttachmentState state) {
          return extractFirstChildren(basePath, state.getPath());
        }
      })).forEach((n, s) -> folders.add(Mica.FileDto.newBuilder().setType(Mica.FileType.FOLDER)
        .setPath(isRoot(basePath) ? String.format("/%s", n) : String.format("%s/%s", basePath, n)).setName(n)
        .setSize(s.size()).build()));
    return folders;
  }

  private Iterable<Mica.FileDto> getChildrenFiles(String basePath) {
    return fileSystemService.findAttachmentStates(String.format("^%s$", basePath), published).stream().map(dtos::asFileDto)
      .collect(Collectors.toList());
  }

  private String extractFirstChildren(String basePath, String path) {
    return path.replaceFirst(String.format("^%s/", isRoot(basePath) ? "" : basePath), "").split("/")[0];
  }

  private boolean isRoot(String basePath) {
    return "/".equals(basePath);
  }

  private String normalizePath(String path) {
    return path.startsWith("/") ? path : String.format("/%s", path);
  }
}
