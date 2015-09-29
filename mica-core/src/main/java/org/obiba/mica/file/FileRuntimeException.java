package org.obiba.mica.file;

public class FileRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 497002696064965475L;

  public FileRuntimeException(String filename) {
    this(filename, null);
  }

  public FileRuntimeException(String filename, Exception cause) {
    super(String.format("File not accessible: %s", filename), cause);
  }
}
