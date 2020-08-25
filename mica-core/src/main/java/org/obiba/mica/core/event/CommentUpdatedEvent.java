package org.obiba.mica.core.event;

import org.obiba.mica.core.domain.Comment;

public class CommentUpdatedEvent {

  private final Comment comment;

  public CommentUpdatedEvent(Comment comment) {
    this.comment = comment;
  }

  public Comment getComment() {
    return comment;
  }
}
