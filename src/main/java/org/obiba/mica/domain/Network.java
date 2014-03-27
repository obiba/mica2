package org.obiba.mica.domain;

import java.io.Serializable;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A Network.
 */
@Document
public class Network extends AbstractTimestampedDocument implements Serializable {

  private static final long serialVersionUID = -4271967393906681773L;

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
    return Objects.equals(id, ((Network) obj).id);
  }

  @Override
  public String toString() {
    return com.google.common.base.Objects.toStringHelper(this).add("id", id).toString();
  }
}
