/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.obiba.mica.core.domain.AttachmentAware;
import org.obiba.mica.file.Attachment;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Document
public class DataAccessRequest extends DataAccessEntity implements AttachmentAware {

  private static final long serialVersionUID = -6728220507676973832L;

  @DBRef
  private List<Attachment> attachments = Lists.newArrayList();

  private Iterable<Attachment> removedAttachments = Lists.newArrayList();

  private Date startDate;

  private boolean archived;

  private boolean lockedByPreliminary = true;

  private List<String> acceptedCollaboratorInvitations = Lists.newArrayList();

  public boolean hasStartDate() {
    return startDate != null;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  private Date getDefaultStartDate() {
    // default is approval date
    for (StatusChange change : getStatusChangeHistory()) {
      if (change.getTo().equals(DataAccessEntityStatus.APPROVED)) {
        return Date.from(change.getChangedOn().atZone(ZoneId.systemDefault()).toInstant());
      }
    }
    return null;
  }

  /**
   * Get the data access request from the startDate field in the data access request or from the approval data, only if
   * the current status is "approved".
   *
   * @return
   */
  public Date getStartDateOrDefault() {
    if (!DataAccessEntityStatus.APPROVED.equals(getStatus())) return null;
    return hasStartDate() ? getStartDate() : getDefaultStartDate();
  }

  /**
   * When preliminary form is enabled, the main form is locked by default until the preliminary form is in a final state
   * (approved or rejected=). This setting bypass this rule so that applicant can fill-in the main form without waiting for
   * the preliminary form to be completed.
   *
   * @param lockedByPreliminary
   */
  public void setLockedByPreliminary(boolean lockedByPreliminary) {
    this.lockedByPreliminary = lockedByPreliminary;
  }

  public boolean isLockedByPreliminary() {
    return lockedByPreliminary;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }

  public boolean isArchived() {
    return archived;
  }

  //
  // Attachments
  //

  @Override
  @NotNull
  public List<Attachment> getAttachments() {
    return attachments;
  }

  @Override
  public boolean hasAttachments() {
    return attachments != null && !attachments.isEmpty();
  }

  @Override
  public void addAttachment(@NotNull Attachment attachment) {
    getAttachments().add(attachment);
  }

  @Override
  public void setAttachments(List<Attachment> attachments) {
    if (attachments == null) attachments = Lists.newArrayList();

    this.removedAttachments = Sets.difference(Sets.newHashSet(this.attachments), Sets.newHashSet(attachments));
    this.attachments = attachments;
  }

  @Override
  public List<Attachment> removedAttachments() {
    return Lists.newArrayList(removedAttachments);
  }

  @JsonIgnore
  @Override
  public Iterable<Attachment> getAllAttachments() {
    return () -> getAttachments().stream().filter(a -> a != null).iterator();
  }

  @Override
  public Attachment findAttachmentById(String attachmentId) {
    return getAttachments().stream().filter(a -> a != null && a.getId().equals(attachmentId)).findAny().orElse(null);
  }

  public List<String> getAcceptedCollaboratorInvitations() {
    return acceptedCollaboratorInvitations == null ? acceptedCollaboratorInvitations = Lists.newArrayList() : acceptedCollaboratorInvitations;
  }

  public void setAcceptedCollaboratorInvitations(List<String> acceptedCollaboratorInvitations) {
    this.acceptedCollaboratorInvitations = acceptedCollaboratorInvitations;
  }

  public void addAcceptedCollaboratorInvitation(String key) {
    if (!hasAcceptedCollaboratorInvitation(key))
      getAcceptedCollaboratorInvitations().add(key);
  }

  public boolean hasAcceptedCollaboratorInvitation(String key) {
    return getAcceptedCollaboratorInvitations().contains(key);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder extends DataAccessEntity.Builder {
    public Builder() {
      request = new DataAccessRequest();
    }
  }

}
