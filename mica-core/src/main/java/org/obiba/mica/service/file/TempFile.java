package org.obiba.mica.service.file;

import java.io.Serializable;
import java.util.Objects;

import javax.validation.constraints.NotNull;

public class TempFile implements Serializable {

  private static final long serialVersionUID = -6939664239667654622L;

  @NotNull
  private String id;

  @NotNull
  private String name;

  private long size;

  @NotNull
  private String md5;

  public TempFile() {
  }

  public TempFile(@NotNull String id, @NotNull String name, long size, @NotNull String md5) {
    this.id = id;
    this.name = name;
    this.size = size;
    this.md5 = md5;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  @Override
  public int hashCode() {return Objects.hash(id);}

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final TempFile other = (TempFile) obj;
    return Objects.equals(this.id, other.id);
  }
}
