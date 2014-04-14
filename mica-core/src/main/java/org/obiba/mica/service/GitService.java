package org.obiba.mica.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.obiba.git.command.AddFilesCommand;
import org.obiba.git.command.GitCommandHandler;
import org.obiba.git.command.ReadFileCommand;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import static com.google.common.base.Charsets.UTF_8;

@Component
public class GitService implements EnvironmentAware {

  @Inject
  private GitCommandHandler gitCommandHandler;

  private RelaxedPropertyResolver propertyResolver;

  private File repositoriesRoot;

  private final Gson gson = new GsonBuilder().create();

  @PostConstruct
  public void init() {
    if(repositoriesRoot == null) repositoriesRoot = new File(propertyResolver.getProperty("repo-path"));
  }

  @Override
  public void setEnvironment(Environment environment) {
    propertyResolver = new RelaxedPropertyResolver(environment, "git.");
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

  public <T> T read(String id, Class<T> clazz) {
    try {
      try(InputStream inputStream = gitCommandHandler
          .execute(new ReadFileCommand.Builder(getRepositoryPath(id), getJsonFileName(clazz)).build());
          JsonReader reader = new JsonReader(new InputStreamReader(inputStream, UTF_8))) {
        return gson.fromJson(reader, clazz);
      }
    } catch(Exception e) {
      throw new RuntimeException("Cannot read " + clazz.getName() + " from " + id + " repo", e);
    }
  }

  private File getRepositoryPath(String id) {
    return new File(repositoriesRoot, id + ".git");
  }

  private String getJsonFileName(Class<?> clazz) {
    return clazz.getSimpleName() + ".json";
  }
}
