package org.obiba.mica.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import static com.google.common.base.Charsets.UTF_8;

@Component
public class GitService {

  private static final File REPO_PATH = new File("target/repo");

  private final Gson gson = new GsonBuilder().create();

  public void save(String id, Object obj) {
    try {
      try(FileOutputStream out = new FileOutputStream(getJsonFile(id, obj.getClass()));
          JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, UTF_8))) {
        writer.setIndent("  ");
        gson.toJson(obj, obj.getClass(), writer);
      }
    } catch(IOException e) {
      throw new RuntimeException("Cannot persist " + obj + " to " + id + " repo");
    }
  }

  public <T> T read(String id, Class<T> clazz) {
    try {
      try(FileInputStream in = new FileInputStream(getJsonFile(id, clazz));
          JsonReader reader = new JsonReader(new InputStreamReader(in, UTF_8))) {
        return gson.fromJson(reader, clazz);
      }
    } catch(IOException e) {
      throw new RuntimeException("Cannot read " + clazz.getName() + " from " + id + " repo");
    }
  }

  private File getJsonFile(String id, Class<?> clazz) {
    return new File(getRepo(id), clazz.getSimpleName() + ".json");
  }

  private File getRepo(String id) {
    File dir = new File(REPO_PATH, id);
    if(!dir.mkdirs()) {
      throw new RuntimeException("Cannot create repo " + dir.getAbsolutePath());
    }
    return dir;
  }

}
