package org.obiba.mica.core.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.PushResult;
import org.joda.time.DateTime;
import org.obiba.git.GitException;
import org.obiba.git.command.AbstractGitWriteCommand;
import org.obiba.git.command.AddDeleteFilesCommand;
import org.obiba.git.command.GitCommandHandler;
import org.obiba.git.command.ListFilesCommand;
import org.obiba.git.command.ReadFileCommand;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.PersistableWithAttachments;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.file.TempFileService;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;

@Component
@Validated
public class GitService {

  private static final String PATH_DATA = "${MICA_SERVER_HOME}/data/git";

  private static final String PATH_CLONES = "${MICA_SERVER_HOME}/work/git";

  private static final String ATTACHMENTS_PATH = "attachments/";

  @Inject
  private GitCommandHandler gitCommandHandler;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private TempFileService tempFileService;

  private File repositoriesRoot;

  private File clonesRoot;

  @PostConstruct
  public void init() {
    if(repositoriesRoot == null) {
      repositoriesRoot = new File(PATH_DATA.replace("${MICA_SERVER_HOME}", System.getProperty("MICA_SERVER_HOME")));
    }
    if(clonesRoot == null) {
      clonesRoot = new File(PATH_CLONES.replace("${MICA_SERVER_HOME}", System.getProperty("MICA_SERVER_HOME")));
    }
  }

  @VisibleForTesting
  public void setRepositoriesRoot(File repositoriesRoot) {
    this.repositoriesRoot = repositoriesRoot;
  }

  @VisibleForTesting
  public void setClonesRoot(File clonesRoot) {
    this.clonesRoot = clonesRoot;
  }

  public boolean hasGitRepository(String id) {
    return FileUtils.fileExists(getRepositoryPath(id).getAbsolutePath())
      || FileUtils.fileExists(getCloneRepositoryPath(id).getAbsolutePath());
  }

  public <TGitPersistable extends AbstractGitPersistable> void save(@NotNull @Valid TGitPersistable persistable) {
    try {

      persistable.setLastModifiedDate(DateTime.now());

      AddDeleteFilesCommand.Builder builder = new AddDeleteFilesCommand.Builder(getRepositoryPath(persistable.getId()),
          clonesRoot, "Update");

      if(persistable instanceof PersistableWithAttachments) {
        processAttachments((PersistableWithAttachments) persistable, builder);
      }

      File jsonFile = serializePersistable(persistable);

      // add this temp JSON file to GIT
      try(InputStream input = new FileInputStream(jsonFile)) {
        builder.addFile(getJsonFileName(persistable.getClass()), input);
        gitCommandHandler.execute(builder.build());
      }

      //noinspection ResultOfMethodCallIgnored
      jsonFile.delete();

    } catch(IOException e) {
      throw new RuntimeException("Cannot persist " + persistable + " to " + persistable.getId() + " repo", e);
    }
  }

  private <TGitPersistable extends AbstractGitPersistable> File serializePersistable(TGitPersistable persistable)
      throws IOException {
    File jsonFile = File.createTempFile("mica", "json");
    jsonFile.deleteOnExit();
    try(FileOutputStream out = new FileOutputStream(jsonFile)) {
      objectMapper.writeValue(out, persistable);
    }
    return jsonFile;
  }

  private void processAttachments(PersistableWithAttachments persistable, AddDeleteFilesCommand.Builder builder)
      throws IOException {

    Collection<String> existingPathsInRepo = getExistingPathsInRepo(persistable);
    Collection<String> filesToDelete = new HashSet<>(existingPathsInRepo);
    persistable.getAllAttachments().forEach(a -> processAttachment(a, persistable, builder, filesToDelete));
    filesToDelete.forEach(builder::deleteFile);
  }

  private Collection<String> getExistingPathsInRepo(Persistable<String> persistable) {
    return gitCommandHandler.execute(
        new ListFilesCommand.Builder(getRepositoryPath(persistable.getId()), clonesRoot).filter(ATTACHMENTS_PATH + "*")
            .recursive(true).build());
  }

  private void processAttachment(Attachment attachment, Persistable<String> parent,
      AddDeleteFilesCommand.Builder builder, Collection<String> filesToDelete) {
    String pathInRepo = getPathInRepo(attachment.getId());
    if(attachment.isJustUploaded()) {
      TempFile tempFile = tempFileService.getMetadata(attachment.getId());
      builder.addFile(pathInRepo, new ByteArrayInputStream(tempFileService.getContent(attachment.getId())));
      attachment.setName(tempFile.getName());
      attachment.setSize(tempFile.getSize());
      attachment.setMd5(tempFile.getMd5());
      attachment.setJustUploaded(false);
      tempFileService.delete(attachment.getId());
    }
    filesToDelete.remove(pathInRepo);
  }

  public <T> T readHead(String id, Class<T> clazz) {
    return read(id, null, clazz);
  }

  public <T> T readFromTag(String id, String tag, Class<T> clazz) {
    return read(id, tag, clazz);
  }

  private <T> T read(String id, @Nullable String tag, Class<T> clazz) {
    try {
      try(InputStream inputStream = gitCommandHandler
          .execute(new ReadFileCommand.Builder(getRepositoryPath(id), getJsonFileName(clazz)).tag(tag).build())) {
        return objectMapper.readValue(inputStream, clazz);
      }
    } catch(IOException e) {
      throw new RuntimeException("Cannot read " + clazz.getName() + " from " + id + " repo", e);
    }
  }

  public byte[] readFileHead(String id, String fileId) {
    return readFile(id, fileId, null);
  }

  public byte[] readFileFromTag(String id, String fileId, String tag) {
    return readFile(id, fileId, tag);
  }

  private byte[] readFile(String id, String fileId, @Nullable String tag) {
    try {
      try(InputStream inputStream = gitCommandHandler
          .execute(new ReadFileCommand.Builder(getRepositoryPath(id), getPathInRepo(fileId)).tag(tag).build())) {
        return ByteStreams.toByteArray(inputStream);
      }
    } catch(IOException e) {
      throw new RuntimeException("Cannot read file " + fileId + " from " + id + " repo", e);
    }
  }

  private String getPathInRepo(String attachmentId) {
    return ATTACHMENTS_PATH + attachmentId;
  }

  public String tag(String id) {
    IncrementTagCommand command = new IncrementTagCommand(getRepositoryPath(id), clonesRoot);
    gitCommandHandler.execute(command);
    return String.valueOf(command.getNewTag());
  }

  private File getRepositoryPath(String id) {
    return new File(repositoriesRoot, id + ".git");
  }

  private File getCloneRepositoryPath(String id) {
    return new File(repositoriesRoot, id );
  }

  private String getJsonFileName(Class<?> clazz) {
    return clazz.getSimpleName() + ".json";
  }

  private static class IncrementTagCommand extends AbstractGitWriteCommand {

    private int newTag = 1;

    private IncrementTagCommand(@NotNull File repositoryPath, @NotNull File clonesPath) {
      super(repositoryPath, clonesPath, null);
    }

    @Override
    public Iterable<PushResult> execute(Git git) {
      try {
        List<Ref> refs = git.tagList().call();
        if(!refs.isEmpty()) {
          Ref lastRef = refs.get(refs.size() - 1);
          String name = lastRef.getName();
          newTag = Integer.valueOf(name.substring(name.lastIndexOf('/') + 1, name.length())) + 1;
        }
        git.tag().setMessage("Create tag " + newTag).setName(String.valueOf(newTag))
            .setTagger(new PersonIdent(getAuthorName(), getAuthorEmail())).call();
        return git.push().setPushTags().setRemote("origin").call();
      } catch(GitAPIException e) {
        throw new GitException(e);
      }
    }

    public int getNewTag() {
      return newTag;
    }
  }
}
