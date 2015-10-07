package org.obiba.mica.file;

import org.joda.time.DateTime;
import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.domain.RevisionStatus;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class AttachmentState extends AbstractAuditableDocument {

  private static final long serialVersionUID = 6265547392853691755L;

  @Indexed
  private RevisionStatus revisionStatus = RevisionStatus.DRAFT;

  private String name;

  private String path;

  @DBRef
  private Attachment attachment;

  @DBRef
  private Attachment publishedAttachment;

  private DateTime publicationDate;

  private String publishedBy;

  //
  // Public methods
  //

  public void setRevisionStatus(RevisionStatus revisionStatus) {
    this.revisionStatus = revisionStatus;
  }

  public RevisionStatus getRevisionStatus() {
    return revisionStatus;
  }

  public void setAttachment(Attachment attachment) {
    this.attachment = attachment;
    name = attachment.getName();
    path = attachment.getPath();
  }

  public Attachment getAttachment() {
    return attachment;
  }

  public void setPublishedBy(String publishedBy) {
    this.publishedBy = publishedBy;
  }

  public String getPublishedBy() {
    return publishedBy;
  }

  public void setPublishedAttachment(Attachment publishedAttachment) {
    this.publishedAttachment = publishedAttachment;
  }

  public Attachment getPublishedAttachment() {
    return publishedAttachment;
  }

  public DateTime getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(DateTime publicationDate) {
    this.publicationDate = publicationDate;
  }

  public boolean isPublished() {
    return publicationDate != null;
  }
}
