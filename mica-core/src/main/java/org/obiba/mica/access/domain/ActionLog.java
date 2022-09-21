package org.obiba.mica.access.domain;

import java.time.LocalDateTime;

public class ActionLog extends ChangeLog {
  private static final long serialVersionUID = -8071043213070449724L;

  private String id;

  private String action;

  public String getAction() {
    return action;
  }

  public static Builder newBuilder(ActionLog source) {
    return new Builder(source);
  }

  public static Builder newBuilder() {
    return newBuilder(null);
  }

  public String getId() {
    return id;
  }

  public static final class Builder {

    private final ActionLog actionLog;

    private Builder(ActionLog source) {
      actionLog = source == null ? new ActionLog() : source;
    }

    public Builder id(String value) {
      actionLog.id = value;
      return this;
    }

    public Builder action(String value) {
      actionLog.action = value;
      return this;
    }

    public Builder author(String value) {
      actionLog.author = value;
      return this;
    }

    public Builder changedOn(LocalDateTime value) {
      this.actionLog.changedOn = value;
      return this;
    }

    public ActionLog build() {
      return actionLog;
    }
  }
}
