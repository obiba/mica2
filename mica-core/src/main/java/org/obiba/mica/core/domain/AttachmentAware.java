/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.obiba.mica.file.Attachment;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Utility interface to manipulate {@link org.obiba.mica.file.Attachment}s.
 */
public interface AttachmentAware {

  boolean hasAttachments();

  List<Attachment> getAttachments();

  void addAttachment(@NotNull Attachment attachment);

  void setAttachments(List<Attachment> attachments);

  @JsonIgnore
  List<Attachment> removedAttachments();

  @JsonIgnore
  Iterable<Attachment> getAllAttachments();

  @NotNull
  Attachment findAttachmentById(String attachmentId);
}
