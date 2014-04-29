package org.obiba.mica.domain;

import javax.validation.constraints.NotNull;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

@Document
public class StudyState extends AbstractAuditableDocument {

  private static final long serialVersionUID = -4271967393906681773L;

  @Indexed
  private RevisionStatus publicationStatus = RevisionStatus.DRAFT;

  @Indexed
  private String publishedTag;

  @NotNull
  private LocalizedString name;

  public String getPublishedTag() {
    return publishedTag;
  }

  public void setPublishedTag(String publishedTag) {
    this.publishedTag = publishedTag;
  }

  public boolean isPublished() {
    return !Strings.isNullOrEmpty(publishedTag);
  }

  public LocalizedString getName() {
    return name;
  }

  public void setName(LocalizedString name) {
    this.name = name;
  }

  public RevisionStatus getPublicationStatus() {
    return publicationStatus;
  }

  public void setPublicationStatus(RevisionStatus publicationStatus) {
    this.publicationStatus = publicationStatus;
  }

  @Override
  protected Objects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("name", name) //
        .add("publicationStatus", publicationStatus) //
        .add("publishedTag", publishedTag);
  }

}
