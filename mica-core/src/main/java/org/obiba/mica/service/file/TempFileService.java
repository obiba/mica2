package org.obiba.mica.service.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

@Component
public class TempFileService {

  private static final Logger log = LoggerFactory.getLogger(TempFileService.class);

  private static final String TMP_ROOT = "${MICA_SERVER_HOME}/work/tmp";

  private static final String DATA_FILE = "file";

  private static final String METADATA_FILE = "metadata.json";

  @Inject
  private ObjectMapper objectMapper;

  private File tmpRoot;

  @PostConstruct
  public void init() {
    if(tmpRoot == null) {
      tmpRoot = new File(TMP_ROOT.replace("${MICA_SERVER_HOME}", System.getProperty("MICA_SERVER_HOME")));
    }
  }

  public TempFile addTempFile(@NotNull String fileName, @NotNull InputStream uploadedInputStream) throws IOException {
    ObjectId id = new ObjectId();
    File dir = getDir(id.toString());
    if(!dir.mkdirs()) {
      throw new IOException("Cannot create temp dir for new temp file: " + dir.getAbsolutePath());
    }
    File file = new File(dir, DATA_FILE);
    ByteStreams.copy(uploadedInputStream, new FileOutputStream(file));
    TempFile tempFile = new TempFile(id.toString(), fileName, file.length(),
        Files.hash(file, Hashing.md5()).toString());

    try(FileOutputStream out = new FileOutputStream(new File(dir, METADATA_FILE))) {
      objectMapper.writeValue(out, tempFile);
    }
    return tempFile;
  }

  public TempFile getMetadata(@NotNull String id) throws IOException {
    return objectMapper.readValue(getMetadataFile(id), TempFile.class);
  }

  public byte[] getContent(@NotNull String id) throws IOException {
    return Files.toByteArray(getFile(id));
  }

  private File getDir(@NotNull String id) {
    return new File(tmpRoot, id);
  }

  private File getMetadataFile(@NotNull String id) {
    return new File(getDir(id), METADATA_FILE);
  }

  private File getFile(@NotNull String id) {
    return new File(getDir(id), DATA_FILE);
  }

  public void delete(@NotNull String id) {
    if(!new File(tmpRoot, id).delete()) {
      log.debug("Could not delete temp file {}", id);
    }
  }
}
