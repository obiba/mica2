/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

public class Comment extends AbstractAuditableDocument {

  private static final long serialVersionUID = -1490617732167157048L;

  private String author;

  private String message;

  private String classId;

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getMessage() {
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

  public String getClassId() {
    return classId;
  }

  public void setClassId(String classAndId) {
    classId = classAndId;
  }

  /**
   * Helper class to build a new comment instance
   */
  public static class Builder {

    private final Comment comment;

    private Builder(Comment source) {
      comment = source == null ? new Comment() : source;
    }

    public static String generateId(Class clazz, String id) {
      return clazz.getSimpleName() + "__" + id;
    }

    public Builder id(String id) {
      comment.setId(id);
      return this;
    }

    public Builder author(String author) {
      comment.setAuthor(author);
      return this;
    }

    public Builder message(String message) {
      comment.setMessage(message);
      return this;
    }

    public Builder classId(String classId) {
      comment.setClassId(classId);
      return this;
    }

    public Builder classId(Class clazz, String id) {
      return classId(generateId(clazz, id));
    }

    public Comment build() {
      return comment;
    }
  }

}
