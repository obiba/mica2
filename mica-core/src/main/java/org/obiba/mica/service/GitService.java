package org.obiba.mica.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.PushResult;
import org.obiba.git.GitException;
import org.obiba.git.command.AbstractGitWriteCommand;
import org.obiba.git.command.AddFilesCommand;
import org.obiba.git.command.GitCommandHandler;
import org.obiba.git.command.ReadFileCommand;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import static com.google.common.base.Charsets.UTF_8;

@Component
public class GitService {

  public static final String PATH_DATA = "${MICA_HOME}/data/git";

  @Inject
  private GitCommandHandler gitCommandHandler;

  @Inject
  private Gson gson;

  private File repositoriesRoot;

  @PostConstruct
  public void init() {
    if(repositoriesRoot == null) {
      repositoriesRoot = new File(PATH_DATA.replace("${MICA_HOME}", System.getProperty("MICA_HOME")));
    }
  }

  public void setRepositoriesRoot(File repositoriesRoot) {
    this.repositoriesRoot = repositoriesRoot;
  }

  public void save(String id, Object obj) {
    try {

      File jsonFile = File.createTempFile("mica", "json");
      jsonFile.deleteOnExit();
      String jsonFileName = getJsonFileName(obj.getClass());

      // write Object to temp JSON file
      try(FileOutputStream out = new FileOutputStream(jsonFile);
          JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, UTF_8))) {
        writer.setIndent("  ");
        gson.toJson(obj, obj.getClass(), writer);
      }

      // add this temp JSON file to GIT
      try(InputStream input = new FileInputStream(jsonFile)) {
        gitCommandHandler.execute(
            new AddFilesCommand.Builder(getRepositoryPath(id), "Update " + jsonFileName).addFile(jsonFileName, input)
                .build()
        );
      }

      //noinspection ResultOfMethodCallIgnored
      jsonFile.delete();

    } catch(IOException e) {
      throw new RuntimeException("Cannot persist " + obj + " to " + id + " repo", e);
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
          .execute(new ReadFileCommand.Builder(getRepositoryPath(id), getJsonFileName(clazz)).tag(tag).build());
          JsonReader reader = new JsonReader(new InputStreamReader(inputStream, UTF_8))) {
        return gson.fromJson(reader, clazz);
      }
    } catch(IOException e) {
      throw new RuntimeException("Cannot read " + clazz.getName() + " from " + id + " repo", e);
    }
  }

  public String tag(String id) {
    IncrementTagCommand command = new IncrementTagCommand(getRepositoryPath(id));
    gitCommandHandler.execute(command);
    return String.valueOf(command.getNewTag());
  }

  private File getRepositoryPath(String id) {
    return new File(repositoriesRoot, id + ".git");
  }

  private String getJsonFileName(Class<?> clazz) {
    return clazz.getSimpleName() + ".json";
  }

  private static class IncrementTagCommand extends AbstractGitWriteCommand {

    private int newTag = 1;

    private IncrementTagCommand(@NotNull File repositoryPath) {
      super(repositoryPath, null);
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
