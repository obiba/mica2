package org.obiba.mica.file.rest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import javafx.util.Pair;

public abstract class AbstractFileSystemResource {

  @Inject
  protected FileSystemService fileSystemService;

  @Inject
  private Dtos dtos;

  /**
   * Specify if the file system view is published or draft. If published, only the published {@link AttachmentState}s
   * will be looked up and the corresponding {@link Attachment} will be returned. Otherwise all {@link AttachmentState}
   * and all revisions of {@link Attachment}s are accessible.
   *
   * @return
   */
  protected abstract boolean isPublished();

  protected List<Mica.FileDto> doSearchFiles(String path, String query, boolean recursively) {
    return getChildrenFiles(normalizePath(path), recursively);
  }

  protected Attachment doGetAttachment(String path) {
    return doGetAttachment(path, null);
  }

  protected Attachment doGetAttachment(String path, @Nullable String version) {
    String basePath = normalizePath(path);
    if(basePath.endsWith("/")) throw new IllegalArgumentException("Folder download is not supported");

    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);
    AttachmentState state = fileSystemService.getAttachmentState(pathName.getKey(), pathName.getValue(), isPublished());
    if(isPublished()) return state.getPublishedAttachment();

    if(Strings.isNullOrEmpty(version)) return state.getAttachment();

    List<Attachment> attachments = fileSystemService.getAttachmentRevisions(state).stream()
      .filter(a -> a.getId().equals(version)).collect(Collectors.toList());

    if(attachments.isEmpty())
      throw new NoSuchElementException("No file version " + version + " found at path: " + basePath);

    return attachments.get(0);
  }

  protected Mica.FileDto doGetFile(String path) {
    String basePath = normalizePath(path);
    if(isRoot(basePath)) return getFolderDto(basePath);
    if(basePath.endsWith("/")) return getFolderDto(basePath);

    try {
      return getFileDto(basePath);
    } catch(NoSuchEntityException ex) {
      return getFolderDto(basePath);
    }
  }

  protected void doDeleteFile(String path) {
    String basePath = normalizePath(path);
    if(isRoot(basePath)) deleteFolderState(basePath);
    if(basePath.endsWith("/")) deleteFolderState(basePath);

    try {
      deleteFileState(basePath);
    } catch(NoSuchEntityException ex) {
      deleteFolderState(basePath);
    }
  }

  protected void doPublishFile(String path, boolean publish) {
    String basePath = normalizePath(path);
    if(isRoot(basePath)) publishFolderState(basePath, publish);
    if(basePath.endsWith("/")) publishFolderState(basePath, publish);

    try {
      publishFileState(basePath, publish);
    } catch(NoSuchEntityException ex) {
      publishFolderState(basePath, publish);
    }
  }

  protected void doAddFile(Mica.AttachmentDto attachmentDto) {
    fileSystemService.save(dtos.fromDto(attachmentDto));
  }

  protected void doRenameFile(String path, @NotNull String newName) {
    String basePath = normalizePath(path);
    if(isRoot(basePath)) throw new IllegalArgumentException("Root folder cannot be renamed");
    if(basePath.endsWith("/")) renameFolderState(basePath, newName);

    try {
      renameFileState(basePath, newName);
    } catch(NoSuchEntityException ex) {
      renameFolderState(basePath, newName);
    }
  }

  protected void doMoveFile(String path, @NotNull String newPath) {
    String basePath = normalizePath(path);
    if(isRoot(basePath)) throw new IllegalArgumentException("Root folder cannot be renamed");
    if(basePath.endsWith("/")) renameFolderState(basePath, newPath);

    try {
      moveFileState(basePath, newPath);
    } catch(NoSuchEntityException ex) {
      renameFolderState(basePath, newPath);
    }
  }

  protected void doCopyFile(String path, @NotNull String newPath) {
    String basePath = normalizePath(path);
    if(isRoot(basePath)) throw new IllegalArgumentException("Root folder cannot be renamed");
    if(basePath.endsWith("/")) copyFolderState(basePath, newPath);

    try {
      copyFileState(basePath, newPath);
    } catch(NoSuchEntityException ex) {
      copyFolderState(basePath, newPath);
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

  private void publishFileState(String basePath, boolean publish) {
    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);
    fileSystemService.publish(pathName.getKey(), pathName.getValue(), publish);
  }

  private void publishFolderState(String basePath, boolean publish) {
    fileSystemService.publish(basePath, publish);
  }

  private void renameFileState(String basePath, String newName) {
    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);
    fileSystemService.rename(pathName.getKey(), pathName.getValue(), newName);
  }

  private void renameFolderState(String basePath, String newName) {
    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);
    fileSystemService.rename(basePath, pathName.getKey() + "/" + newName);
  }

  private void moveFileState(String basePath, String newPath) {
    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);
    fileSystemService.move(pathName.getKey(), pathName.getValue(), newPath);
  }

  private void copyFileState(String basePath, String newPath) {
    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);
    fileSystemService.copy(pathName.getKey(), pathName.getValue(), newPath);
  }

  private void copyFolderState(String basePath, String newPath) {
    fileSystemService.copy(basePath, newPath);
  }

  private Mica.FileDto getFileDto(String basePath) {
    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);
    AttachmentState state = fileSystemService.getAttachmentState(pathName.getKey(), pathName.getValue(), isPublished());
    return dtos.asFileDto(state, isPublished());
  }

  private Mica.FileDto getFolderDto(String basePath) {
    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);

    Mica.FileDto.Builder builder = Mica.FileDto.newBuilder();
    builder.addAllChildren(getChildrenFolders(basePath)) //
      .addAllChildren(getChildrenFiles(basePath, false));

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
    fileSystemService.findAttachmentStates(pathRegEx, isPublished()).stream() //
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

  private List<Mica.FileDto> getChildrenFiles(String basePath, boolean recursively) {
    List<AttachmentState> states = fileSystemService
      .findAttachmentStates(String.format("^%s$", basePath), isPublished()).stream().collect(Collectors.toList());

    if(recursively) {
      states.addAll(fileSystemService.findAttachmentStates(String.format("^%s/", basePath), isPublished()).stream()
        .collect(Collectors.toList()));
    }

    return states.stream().map(dtos::asFileDto).collect(Collectors.toList());
  }

  private String extractFirstChildren(String basePath, String path) {
    return path.replaceFirst(String.format("^%s/", isRoot(basePath) ? "" : basePath), "").split("/")[0];
  }

  private boolean isRoot(String basePath) {
    return "/".equals(basePath);
  }

  private String normalizePath(String path) {
    String nPath = path.startsWith("/") ? path : String.format("/%s", path);
    if (!isRoot(nPath) && nPath.endsWith("/")) nPath = nPath.replaceAll("[/]+$", "");
    return nPath;
  }

}
