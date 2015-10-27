package org.obiba.mica.file.rest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.math3.util.Pair;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import static org.obiba.mica.file.FileUtils.isRoot;
import static org.obiba.mica.file.FileUtils.normalizePath;

public abstract class AbstractFileSystemResource {

  @Inject
  protected SubjectAclService subjectAclService;

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
  protected abstract boolean isPublishedFileSystem();

  protected List<Mica.FileDto> doSearchFiles(String path, String query, boolean recursively) {
    return getChildrenFiles(normalizePath(path), recursively);
  }

  protected Attachment doGetAttachment(String path) {
    return doGetAttachment(path, null);
  }

  protected Attachment doGetAttachment(String path, @Nullable String version) {
    String basePath = normalizePath(path);
    if(path.endsWith("/")) throw new IllegalArgumentException("Folder download is not supported");

    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);
    AttachmentState state = fileSystemService
      .getAttachmentState(pathName.getKey(), pathName.getValue(), isPublishedFileSystem());
    if(isPublishedFileSystem()) return state.getPublishedAttachment();

    if(Strings.isNullOrEmpty(version)) return state.getAttachment();

    List<Attachment> attachments = fileSystemService.getAttachmentRevisions(state).stream()
      .filter(a -> a.getId().equals(version)).collect(Collectors.toList());

    if(attachments.isEmpty())
      throw new NoSuchElementException("No file version " + version + " found at path: " + basePath);

    return attachments.get(0);
  }

  protected Mica.FileDto doGetFile(String path) {
    String basePath = normalizePath(path);
    if(!isRoot(basePath)) subjectAclService
      .checkPermission(String.format("%s/file", isPublishedFileSystem() ? "" : "/draft"),
        "VIEW", basePath);
    if(path.endsWith("/")) return getFolderDto(basePath);

    try {
      return getFileDto(basePath);
    } catch(NoSuchEntityException ex) {
      return getFolderDto(basePath);
    }
  }

  protected void doDeleteFile(String path) {
    String basePath = normalizePath(path);
    if(path.endsWith("/")) deleteFolderState(basePath);

    try {
      deleteFileState(basePath);
    } catch(NoSuchEntityException ex) {
      deleteFolderState(basePath);
    }
  }

  protected void doPublishFile(String path, boolean publish) {
    String basePath = normalizePath(path);
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
    if(path.endsWith("/")) renameFolderState(basePath, newName);

    try {
      renameFileState(basePath, newName);
    } catch(NoSuchEntityException ex) {
      renameFolderState(basePath, newName);
    }
  }

  protected void doMoveFile(String path, @NotNull String newPath) {
    String basePath = normalizePath(path);
    if(isRoot(basePath)) throw new IllegalArgumentException("Root folder cannot be renamed");
    if(path.endsWith("/")) renameFolderState(basePath, newPath);

    try {
      moveFileState(basePath, newPath);
    } catch(NoSuchEntityException ex) {
      renameFolderState(basePath, newPath);
    }
  }

  protected void doCopyFile(String path, @NotNull String newPath) {
    String basePath = normalizePath(path);
    if(isRoot(basePath)) throw new IllegalArgumentException("Root folder cannot be renamed");
    if(path.endsWith("/")) copyFolderState(basePath, newPath);

    try {
      copyFileState(basePath, newPath);
    } catch(NoSuchEntityException ex) {
      copyFolderState(basePath, newPath);
    }
  }

  protected void doUpdateStatus(String path, @NotNull RevisionStatus status) {
    String basePath = normalizePath(path);
    if(path.endsWith("/")) updateFolderStatus(basePath, status);

    try {
      updateFileStatus(basePath, status);
    } catch(NoSuchEntityException ex) {
      updateFolderStatus(basePath, status);
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

  private void updateFileStatus(String basePath, @NotNull RevisionStatus status) {
    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);
    fileSystemService.updateStatus(pathName.getKey(), pathName.getValue(), status);
  }

  private void updateFolderStatus(String basePath, @NotNull RevisionStatus status) {
    fileSystemService.updateStatus(basePath, status);
  }

  private Mica.FileDto getFileDto(String basePath) {
    Pair<String, String> pathName = FileSystemService.extractPathName(basePath);
    AttachmentState state = fileSystemService
      .getAttachmentState(pathName.getKey(), pathName.getValue(), isPublishedFileSystem());
    return dtos.asFileDto(state, isPublishedFileSystem());
  }

  private Mica.FileDto getFolderDto(String basePath) {
    AttachmentState state = fileSystemService
      .getAttachmentState(basePath, FileSystemService.DIR_NAME, isPublishedFileSystem());

    Mica.FileDto.Builder builder = dtos.asFileDto(state, isPublishedFileSystem()).toBuilder();
    builder.addAllChildren(getChildrenFolders(basePath)) //
      .addAllChildren(getChildrenFiles(basePath, false));

    return builder.build();
  }

  private Iterable<Mica.FileDto> getChildrenFolders(String basePath) {
    List<Mica.FileDto> folders = Lists.newArrayList();
    String pathRegEx = isRoot(basePath) ? "^/[^/]+$" : String.format("^%s/[^/]+$", basePath);
    fileSystemService.findAttachmentStates(pathRegEx, isPublishedFileSystem()).stream() //
      .filter(FileUtils::isDirectory) //
      .sorted((o1, o2) -> o1.getPath().compareTo(o2.getPath())) //
      .forEach(s -> folders.add(dtos.asFileDto(s, isPublishedFileSystem(), false)));
    return folders;
  }

  private List<Mica.FileDto> getChildrenFiles(String basePath, boolean recursively) {
    List<AttachmentState> states = fileSystemService
      .findAttachmentStates(String.format("^%s$", basePath), isPublishedFileSystem()).stream()
      .filter(s -> !FileUtils.isDirectory(s)).collect(Collectors.toList());

    if(recursively) {
      states.addAll(
        fileSystemService.findAttachmentStates(String.format("^%s/", basePath), isPublishedFileSystem()).stream()
          .filter(s -> !FileUtils.isDirectory(s)).collect(Collectors.toList()));
    }

    return states.stream() //
      .sorted((o1, o2) -> o1.getFullPath().compareTo(o2.getFullPath())) //
      .map(s -> {
        Mica.FileDto f = dtos.asFileDto(s, isPublishedFileSystem(), false);
        if(isPublishedFileSystem()) f = f.toBuilder().clearRevisionStatus().build();
        return f;
      }).collect(Collectors.toList());
  }

}
