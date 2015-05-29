package org.obiba.mica.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

@Component
public class TempFileService {

  private static final Logger log = LoggerFactory.getLogger(TempFileService.class);

  private static final String TMP_ROOT = "${MICA_HOME}/work/tmp";

  @Inject
  private TempFileRepository tempFileRepository;

  private File tmpRoot;

  @PostConstruct
  public void init() throws IOException {
    if(tmpRoot == null) {
      tmpRoot = new File(TMP_ROOT.replace("${MICA_HOME}", System.getProperty("MICA_HOME")));
      if(!tmpRoot.exists() && !tmpRoot.mkdirs()) {
        throw new IOException("Cannot create temp dir for new temp file: " + tmpRoot.getAbsolutePath());
      }
    }
  }

  @VisibleForTesting
  public void setTmpRoot(File tmpRoot) {
    this.tmpRoot = tmpRoot;
  }

  @NotNull
  public TempFile addTempFile(@NotNull String fileName, @NotNull InputStream uploadedInputStream) throws IOException {
    TempFile tempFile = new TempFile();
    tempFile.setName(fileName);

    return addTempFile(tempFile, uploadedInputStream);
  }

  @NotNull
  public TempFile addTempFile(@NotNull TempFile tempFile, @NotNull InputStream uploadedInputStream) throws IOException {
    TempFile savedTempFile;
    if (tempFile.getId() != null) {
      savedTempFile = tempFileRepository.findOne(tempFile.getId());
      if (savedTempFile == null) {
        savedTempFile = tempFileRepository.save(tempFile);
      }
    } else {
      savedTempFile = tempFileRepository.save(tempFile);
    }

    File file = getFile(savedTempFile.getId());
    OutputStream fileOut = new FileOutputStream(file);
    ByteStreams.copy(uploadedInputStream, fileOut);
    fileOut.close();
    savedTempFile.setSize(file.length());
    savedTempFile.setMd5(Files.hash(file, Hashing.md5()).toString());
    tempFileRepository.save(savedTempFile);
    return savedTempFile;
  }

  @NotNull
  public TempFile getMetadata(@NotNull String id) throws NoSuchTempFileException {
    TempFile tempFile = tempFileRepository.findOne(id);
    if(tempFile == null) throw new NoSuchTempFileException(id);
    return tempFile;
  }

  public byte[] getContent(@NotNull String id) throws NoSuchTempFileException {
    try {
      // check that this tempFile exists
      getMetadata(id);
      return Files.toByteArray(getFile(id));
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  public InputStream getInputStreamFromFile(@NotNull String id) {
    try {
      return new FileInputStream(getFile(id));
    } catch(FileNotFoundException e) {
      throw Throwables.propagate(e);
    }
  }

  private File getFile(@NotNull String id) {
    return new File(tmpRoot, id);
  }

  public void delete(@NotNull String id) {
    tempFileRepository.delete(id);

    if(!getFile(id).delete()) {
      log.debug("Could not delete temp file {}", id);
    }
  }
}
