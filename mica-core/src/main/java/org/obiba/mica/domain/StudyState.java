package org.obiba.mica.domain;

import java.io.Serializable;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class StudyState extends AbstractTimestampedDocument implements Serializable {

  private static final long serialVersionUID = -4271967393906681773L;

  @Id
  private String id;

  @Version
  private Long version;

  @Indexed
  private String publishedTag;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public String getPublishedTag() {
    return publishedTag;
  }

  public void setPublishedTag(String publishedTag) {
    this.publishedTag = publishedTag;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  @SuppressWarnings("SimplifiableIfStatement")
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null || getClass() != obj.getClass()) return false;
    return Objects.equals(id, ((StudyState) obj).id);
  }

  @Override
  public String toString() {
    return com.google.common.base.Objects.toStringHelper(this).add("id", id).add("publishedTag", publishedTag)
        .toString();
  }

}
