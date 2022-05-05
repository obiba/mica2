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

import org.springframework.web.util.HtmlUtils;

public class Comment extends AbstractAuditableDocument {

  private static final long serialVersionUID = -1490617732167157048L;

  private String message;

  // resource id used for permissions
  private String resourceId;

  private String instanceId;

  private boolean admin;

  public String getMessage() {
    // clean messages that were saved as json string instead of plain text
    if (message.startsWith("&quot;") && message.endsWith("&quot;"))
      return message.substring(6, message.length() - 6).replaceAll("\\\\n", "\\n");
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public static Builder newBuilder(Comment comment) {
    return new Builder(comment);
  }

  public static Builder newBuilder() {
    return new Builder(null);
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String name) {
    resourceId = name;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public boolean getAdmin() {
    return admin;
  }

  public void setAdmin(boolean admin) {
    this.admin = admin;
  }

  /**
   * Helper class to build a new comment instance
   */
  public static class Builder {

    private final Comment comment;

    private Builder(Comment source) {
      comment = source == null ? new Comment() : source;
    }

    public Builder id(String id) {
      comment.setId(id);
      return this;
    }

    public Builder createdBy(String author) {
      comment.setCreatedBy(author);
      return this;
    }

    public Builder modifiedBy(String author) {
      comment.setLastModifiedBy(author);
      return this;
    }

    public Builder message(String message) {
      comment.setMessage(HtmlUtils.htmlEscape(message));
      return this;
    }

    public Builder resourceId(String name) {
      comment.setResourceId(name);
      return this;
    }

    public Builder instanceId(String id) {
      comment.setInstanceId(id);
      return this;
    }

    public Builder admin(Boolean admin) {
      comment.setAdmin(admin);
      return this;
    }

    public Comment build() {
      return comment;
    }
  }

}
