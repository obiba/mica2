package org.obiba.mica.web.controller.domain;

import java.time.LocalDateTime;

import org.obiba.mica.core.domain.Comment;

public class TimelineItem {

  private Comment comment;

  private FormStatusChangeEvent event;

  private boolean canRemove;

  public TimelineItem(Comment comment) {
    this.comment = comment;
  }

  public TimelineItem(FormStatusChangeEvent event) {
    this.event = event;
  }

  public boolean isCommentItem() {
    return comment != null;
  }

  public Comment getComment() {
    return comment;
  }

  public boolean isCanRemove() {
    return canRemove;
  }

  public void setCanRemove(boolean canRemove) {
    this.canRemove = canRemove;
  }

  public boolean isEventItem() {
    return event != null;
  }

  public FormStatusChangeEvent getEvent() {
    return event;
  }

  public String getAuthor() {
    return isCommentItem() ? comment.getCreatedBy().get() : event.getAuthor();
  }

  public LocalDateTime getDate() {
    return isCommentItem() ? comment.getCreatedDate().get() : event.getDate();
  }

}
