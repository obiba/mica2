package org.obiba.mica.core.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
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
import org.obiba.git.command.ReadFileCommand;
import org.obiba.mica.core.domain.GitPersistable;
import org.obiba.mica.file.TempFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;

@Component
@Validated
public class GitService {

  private static final Logger log = LoggerFactory.getLogger(GitService.class);

  private static final String PATH_DATA = "${MICA_HOME}/data/git";

  private static final String PATH_CLONES = "${MICA_HOME}/work/git";

  private static final String ATTACHMENTS_PATH = "attachments/";

  @Inject
  private GitCommandHandler gitCommandHandler;

  @Inject
  private ObjectMapper objectMapper;

  private File repositoriesRoot;

  private File clonesRoot;

  @PostConstruct
  public void init() {
    if(repositoriesRoot == null) {
      repositoriesRoot = new File(PATH_DATA.replace("${MICA_HOME}", System.getProperty("MICA_HOME")));
    }
    if(clonesRoot == null) {
      clonesRoot = new File(PATH_CLONES.replace("${MICA_HOME}", System.getProperty("MICA_HOME")));
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

  public boolean hasGitRepository(GitPersistable persistable) {
    return FileUtils.fileExists(getRepositoryPath(persistable).getAbsolutePath()) ||
      FileUtils.fileExists(getCloneRepositoryPath(persistable).getAbsolutePath());
  }

  public void deleteGitRepository(GitPersistable persistable) {
    try {
      FileUtils.deleteDirectory(getRepositoryPath(persistable));
      FileUtils.deleteDirectory(getCloneRepositoryPath(persistable));
    } catch(IOException e) {
      Throwables.propagate(e);
    }
  }

  public void save(
    @NotNull @Valid GitPersistable persistable) {
    persistable.setLastModifiedDate(DateTime.now());

    AddDeleteFilesCommand.Builder builder = new AddDeleteFilesCommand.Builder(getRepositoryPath(persistable),
      new File(clonesRoot, persistable.pathPrefix()) , "Update");

    persistable.parts().entrySet().forEach(p -> {
      try {
        builder.addFile(getJsonFileName(p.getKey()), serializePersistable(p.getValue()));
        //noinspection ResultOfMethodCallIgnored
      }
      catch(IOException e) {
        throw new RuntimeException("Cannot persist " + persistable + " to " + persistable.getId() + " repo", e);
      }
    });

    gitCommandHandler.execute(builder.build());
  }

  private ByteArrayInputStream serializePersistable(
    Object persistable) throws IOException {
    return new ByteArrayInputStream(objectMapper.writeValueAsBytes(persistable));
  }

  public <T> T readFromTag(GitPersistable persistable, String tag, Class<T> clazz) {
    return read(persistable, tag, clazz);
  }

  private <T> T read(GitPersistable persistable, @Nullable String tag, Class<T> clazz) {
    try {
      try(InputStream inputStream = gitCommandHandler
        .execute(new ReadFileCommand.Builder(getRepositoryPath(persistable), getJsonFileName(clazz.getSimpleName())).tag(tag).build())) {
        return objectMapper.readValue(inputStream, clazz);
      }
    } catch(IOException e) {
      throw new RuntimeException("Cannot read " + clazz.getSimpleName() + " from " + persistable + " repo", e);
    }
  }

  public byte[] readFileHead(GitPersistable persistable, String fileId) {
    return readFile(persistable, fileId, null);
  }

  private byte[] readFile(GitPersistable persistable, String fileId, @Nullable String tag) {
    try {
      try(InputStream inputStream = gitCommandHandler
        .execute(new ReadFileCommand.Builder(getRepositoryPath(persistable), getPathInRepo(fileId)).tag(tag).build())) {
        return ByteStreams.toByteArray(inputStream);
      }
    } catch(IOException e) {
      throw new RuntimeException("Cannot read file " + fileId + " from " + persistable + " repo", e);
    }
  }

  private String getPathInRepo(String attachmentId) {
    return ATTACHMENTS_PATH + attachmentId;
  }

  public String tag(GitPersistable persistable) {
    IncrementTagCommand command = new IncrementTagCommand(getRepositoryPath(persistable), new File(clonesRoot, persistable.pathPrefix()));
    gitCommandHandler.execute(command);

    return String.valueOf(command.getNewTag());
  }

  private File getRepositoryPath(GitPersistable persistable) {
    return new File(repositoriesRoot, Paths.get(persistable.pathPrefix(), persistable.getId() + ".git").toString());
  }

  private File getCloneRepositoryPath(GitPersistable persistable) {
    return new File(clonesRoot, Paths.get(persistable.pathPrefix(), persistable.getId()).toString());
  }

  private String getJsonFileName(String filename) {
    return filename + ".json";
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
          int maxTagNumber = 0;
          for(Ref ref : refs) {
            String name = ref.getName();
            try {
              int tagNumber = Integer.valueOf(name.substring(name.lastIndexOf('/') + 1, name.length()));
              if(tagNumber > maxTagNumber) {
                maxTagNumber = tagNumber;
              }
            } catch(NumberFormatException e) {
              // ignore
            }
          }
          newTag = maxTagNumber + 1;
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
