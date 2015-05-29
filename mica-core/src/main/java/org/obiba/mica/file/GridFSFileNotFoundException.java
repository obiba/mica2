package org.obiba.mica.file;

public class GridFSFileNotFoundException extends RuntimeException {

  public GridFSFileNotFoundException(String filename) {
    super(String.format("GridFSFile not found: {}", filename));
  }
}
