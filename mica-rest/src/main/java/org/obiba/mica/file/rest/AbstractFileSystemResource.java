/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.rest;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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

  protected Attachment doGetAttachment(String path) {
    return doGetAttachment(path, null);
  }

  protected Attachment doGetAttachment(String path, @Nullable String version) {
    return doGetAttachment(path, version, null);
  }

  protected Attachment doGetAttachment(String path, @Nullable String version, @Nullable String shareKey) {
    String basePath = normalizePath(path);
    if(isPublishedFileSystem()) subjectAclService.checkAccess("/file", basePath);
    else subjectAclService.checkPermission("/draft/file", "VIEW", basePath, shareKey);

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

  protected Mica.FileDto doGetFile(String path, boolean recursively) {
    return doGetFile(path, null, recursively);
  }

  protected Mica.FileDto doGetFile(String path, String shareKey, boolean recursively) {
    String basePath = normalizePath(path);
    if(isPublishedFileSystem()) subjectAclService.checkAccess("/file", basePath);
    else subjectAclService.checkPermission("/draft/file", "VIEW", basePath, shareKey);

    if(path.endsWith("/")) return getFolderDto(basePath, recursively);

    try {
      return getFileDto(basePath);
    } catch(NoSuchEntityException ex) {
      return getFolderDto(basePath, recursively);
    }
  }

  protected void doDeleteFile(String path) {
    String basePath = normalizePath(path);
    subjectAclService.checkPermission("/draft/file", "DELETE", basePath);
    if(path.endsWith("/")) deleteFolderState(basePath);

    try {
      deleteFileState(basePath);
    } catch(NoSuchEntityException ex) {
      deleteFolderState(basePath);
    }
  }

  protected void doPublishFile(String path, boolean publish) {
    String basePath = normalizePath(path);
    subjectAclService.checkPermission("/draft/file", "PUBLISH", basePath);
    if(basePath.endsWith("/")) publishFolderState(basePath, publish);

    try {
      publishFileState(basePath, publish);
    } catch(NoSuchEntityException ex) {
      publishFolderState(basePath, publish);
    }
  }

  protected void doAddOrUpdateFile(Mica.AttachmentDto attachmentDto) {
    String action = fileSystemService
      .hasAttachmentState(attachmentDto.getPath(), attachmentDto.getFileName(), isPublishedFileSystem())
      ? "EDIT"
      : "ADD";
    subjectAclService.checkPermission("/draft/file", action, attachmentDto.getPath());

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
    subjectAclService.checkPermission("/draft/file", "EDIT", basePath);
    subjectAclService.checkPermission("/draft/file", "ADD", newPath);
    if(isRoot(basePath)) throw new IllegalArgumentException("Root folder cannot be renamed");
    if(path.endsWith("/")) moveFolderState(basePath, newPath);

    try {
      moveFileState(basePath, newPath);
    } catch(NoSuchEntityException ex) {
      moveFolderState(basePath, newPath);
    }
  }

  protected void doCopyFile(String path, @NotNull String newPath) {
    String basePath = normalizePath(path);
    subjectAclService.checkPermission("/draft/file", "VIEW", basePath);
    subjectAclService.checkPermission("/draft/file", "ADD", newPath);
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

    subjectAclService.checkPermission("/draft/file", "EDIT", basePath);

    if(path.endsWith("/")) updateFolderStatus(basePath, status);

    try {
      updateFileStatus(basePath, status);
    } catch(NoSuchEntityException ex) {
      updateFolderStatus(basePath, status);
    }
  }

  protected String doZip(String path) {
    return fileSystemService.zipDirectory(normalizePath(path), isPublishedFileSystem());
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

  private void moveFolderState(String basePath, String newPath) {
    fileSystemService.rename(basePath, newPath);
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

  private Mica.FileDto getFolderDto(String basePath, boolean recursively) {
    AttachmentState state = fileSystemService
      .getAttachmentState(basePath, FileSystemService.DIR_NAME, isPublishedFileSystem());

    return getFolderDto(state, recursively);
  }

  private Mica.FileDto getFolderDto(AttachmentState state, boolean recursively) {
    Mica.FileDto.Builder builder = dtos.asFileDto(state, isPublishedFileSystem()).toBuilder();
    builder.addAllChildren(getChildrenFolders(state.getPath(), recursively))
      .addAllChildren(getChildrenFiles(state.getPath()));

    return builder.build();
  }

  private Iterable<Mica.FileDto> getChildrenFolders(String basePath, boolean recursively) {
    List<Mica.FileDto> folders = Lists.newArrayList();
    String pathRegEx = isRoot(basePath) ? "^/[^/]+$" : String.format("^%s/[^/]+$", basePath);
    fileSystemService.findAttachmentStates(pathRegEx, isPublishedFileSystem()).stream()
      .filter(FileUtils::isDirectory)
      .filter(s -> !isPublishedFileSystem() || subjectAclService.isAccessible("/file", s.getFullPath()))
      .sorted(Comparator.comparing(AttachmentState::getPath))
      .forEach(s -> {
        if (recursively) {
          folders.add(getFolderDto(s, recursively));
        } else {
          folders.add(dtos.asFileDto(s, isPublishedFileSystem(), false));
        }
      });

    return folders;
  }

  private List<Mica.FileDto> getChildrenFiles(String basePath) {
    List<AttachmentState> states = fileSystemService
      .findAttachmentStates(String.format("^%s$", basePath), isPublishedFileSystem()).stream()
      .filter(s -> !FileUtils.isDirectory(s))
      .filter(s -> !isPublishedFileSystem() || subjectAclService.isAccessible("/file", s.getFullPath()))
      .collect(Collectors.toList());

    return states.stream()
      .sorted(Comparator.comparing(AttachmentState::getFullPath))
      .map(s -> {
        Mica.FileDto f = dtos.asFileDto(s, isPublishedFileSystem(), false);
        if(isPublishedFileSystem()) f = f.toBuilder().clearRevisionStatus().build();
        return f;
      }).collect(Collectors.toList());
  }

}
