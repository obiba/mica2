package org.obiba.mica.core.event;

import org.obiba.mica.core.domain.Comment;

public class CommentDeletedEvent {

  private final Comment comment;

  public CommentDeletedEvent(Comment comment) {
    this.comment = comment;
  }

  public Comment getComment() {
    return comment;
  }
}
