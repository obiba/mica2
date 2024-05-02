/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file;

import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.domain.Timestamped;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Document
public class AttachmentState extends AbstractAuditableDocument implements Timestamped {

  private static final long serialVersionUID = 6265547392853691755L;

  @Indexed
  private RevisionStatus revisionStatus = RevisionStatus.DRAFT;

  private String name;

  private String path;

  @DBRef
  private Attachment attachment;

  @DBRef
  private Attachment publishedAttachment;

  private LocalDateTime publicationDate;

  private String publishedBy;

  //
  // Public methods
  //

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  @JsonIgnore
  public String getFullPath() {
    return FileUtils.isDirectory(this) ? path : String.format("%s/%s", path, name);
  }

  public void setPath(String path) {
    this.path = path;
  }

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

  public LocalDateTime getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(LocalDateTime publicationDate) {
    this.publicationDate = publicationDate;
  }

  public void publish(String by) {
    setPublishedAttachment(getAttachment());
    setPublicationDate(LocalDateTime.now());
    setPublishedBy(by);
  }

  public void unPublish() {
    setPublishedAttachment(null);
    setPublicationDate(null);
    setPublishedBy(null);
  }

  @JsonIgnore
  public boolean isPublished() {
    return publicationDate != null && publishedAttachment != null;
  }
}
