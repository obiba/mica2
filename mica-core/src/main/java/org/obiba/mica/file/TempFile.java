package org.obiba.mica.file;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.AbstractAuditableDocument;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class TempFile extends AbstractAuditableDocument {

  private static final long serialVersionUID = -6939664239667654622L;

  @NotNull
  private String name;

  private long size;

  private String md5;

  public TempFile() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

}
