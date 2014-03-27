package org.obiba.mica.domain;

import java.io.Serializable;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A Study.
 */
@Document
public class Study extends AbstractTimestampedDocument implements Serializable {

  private static final long serialVersionUID = 6559914069652243954L;

  @Id
  private String id;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public int hashCode() {return Objects.hash(id);}

  @Override
  @SuppressWarnings("SimplifiableIfStatement")
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null || getClass() != obj.getClass()) return false;
    return Objects.equals(id, ((Study) obj).id);
  }

  @Override
  public String toString() {
    return com.google.common.base.Objects.toStringHelper(this).add("id", id).toString();
  }
}
