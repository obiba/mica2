package org.obiba.mica.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

@Component
public class TempFileService {

  private static final Logger log = LoggerFactory.getLogger(TempFileService.class);

  private static final String TMP_ROOT = "${MICA_SERVER_HOME}/work/tmp";

  @Inject
  private TempFileRepository tempFileRepository;

  private File tmpRoot;

  @PostConstruct
  public void init() throws IOException {
    if(tmpRoot == null) {
      tmpRoot = new File(TMP_ROOT.replace("${MICA_SERVER_HOME}", System.getProperty("MICA_SERVER_HOME")));
      if(!tmpRoot.mkdirs()) {
        throw new IOException("Cannot create temp dir for new temp file: " + tmpRoot.getAbsolutePath());
      }
    }
  }

  @NotNull
  public TempFile addTempFile(@NotNull String fileName, @NotNull InputStream uploadedInputStream) throws IOException {

    TempFile tempFile = new TempFile();
    tempFile.setName(fileName);
    tempFile = tempFileRepository.save(tempFile);

    File file = getFile(tempFile.getId());
    ByteStreams.copy(uploadedInputStream, new FileOutputStream(file));
    tempFile.setSize(file.length());
    tempFile.setMd5(Files.hash(file, Hashing.md5()).toString());
    tempFileRepository.save(tempFile);
    return tempFile;
  }

  @NotNull
  public TempFile getMetadata(@NotNull String id) throws NoSuchTempFileException {
    TempFile tempFile = tempFileRepository.findOne(id);
    if(tempFile == null) throw new NoSuchTempFileException(id);
    return tempFile;
  }

  public byte[] getContent(@NotNull String id) throws NoSuchTempFileException, IOException {
    return Files.toByteArray(getFile(id));
  }

  private File getFile(@NotNull String id) {
    return new File(tmpRoot, id);
  }

  public void delete(@NotNull String id) {
    if(!getFile(id).delete()) {
      log.debug("Could not delete temp file {}", id);
    }
  }
}
