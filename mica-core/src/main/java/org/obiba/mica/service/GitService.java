package org.obiba.mica.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.PushResult;
import org.joda.time.DateTime;
import org.obiba.git.GitException;
import org.obiba.git.command.AbstractGitWriteCommand;
import org.obiba.git.command.AddFilesCommand;
import org.obiba.git.command.GitCommandHandler;
import org.obiba.git.command.ReadFileCommand;
import org.obiba.mica.domain.AbstractGitPersistable;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

@Component
@Validated
public class GitService {

  public static final String PATH_DATA = "${MICA_SERVER_HOME}/data/git";

  public static final String PATH_CLONES = "${MICA_SERVER_HOME}/work/git";

  @Inject
  private GitCommandHandler gitCommandHandler;

  @Inject
  private ObjectMapper objectMapper;

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

  public void save(@NotNull @Valid AbstractGitPersistable persistable) {
    try {

      persistable.setLastModifiedDate(DateTime.now());

      File jsonFile = File.createTempFile("mica", "json");
      jsonFile.deleteOnExit();
      String jsonFileName = getJsonFileName(persistable.getClass());

      // write Object to temp JSON file
      try(FileOutputStream out = new FileOutputStream(jsonFile)) {
        objectMapper.writeValue(out, persistable);
      }

      // add this temp JSON file to GIT
      try(InputStream input = new FileInputStream(jsonFile)) {
        gitCommandHandler.execute(
            new AddFilesCommand.Builder(getRepositoryPath(persistable.getId()), "Update " + jsonFileName)
                .addFile(jsonFileName, input).build());
      }

      //noinspection ResultOfMethodCallIgnored
      jsonFile.delete();

    } catch(IOException e) {
      throw new RuntimeException("Cannot persist " + persistable + " to " + persistable.getId() + " repo", e);
    }
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

  public String tag(String id) {
    IncrementTagCommand command = new IncrementTagCommand(getRepositoryPath(id), getClonePath(id));
    gitCommandHandler.execute(command);
    return String.valueOf(command.getNewTag());
  }

  private File getRepositoryPath(String id) {
    return new File(repositoriesRoot, id + ".git");
  }

  private File getClonePath(String id) {
    return new File(clonesRoot, id);
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
