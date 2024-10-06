package org.obiba.mica.search.basic;

import org.obiba.mica.spi.search.Identified;
import org.obiba.mica.spi.search.Searcher;

import java.io.InputStream;
import java.util.Map;

public class IdentifiedDocumentResult<T extends Identified> implements Searcher.DocumentResult {

  private final T document;

  public IdentifiedDocumentResult(T document) {
    this.document = document;
  }

  @Override
  public String getId() {
    return document.getId();
  }

  @Override
  public boolean hasObject() {
    return true;
  }

  @Override
  public Object getObject() {
    return document;
  }

  @Override
  public boolean hasSource() {
    return false;
  }

  @Override
  public Map<String, Object> getSource() {
    return null;
  }

  @Override
  public InputStream getSourceInputStream() {
    return null;
  }

  @Override
  public String getClassName() {
    return "";
  }
}
