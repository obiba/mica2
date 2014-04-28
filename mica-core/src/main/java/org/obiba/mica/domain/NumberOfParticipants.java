package org.obiba.mica.domain;

import java.io.Serializable;

public class NumberOfParticipants implements Serializable {

  private static final long serialVersionUID = -9146572889284008519L;

  private TargetNumber participant;

  private TargetNumber sample;

  private LocalizedString info;

  public TargetNumber getParticipant() {
    return participant;
  }

  public void setParticipant(TargetNumber participant) {
    this.participant = participant;
  }

  public TargetNumber getSample() {
    return sample;
  }

  public void setSample(TargetNumber sample) {
    this.sample = sample;
  }

  public LocalizedString getInfo() {
    return info;
  }

  public void setInfo(LocalizedString info) {
    this.info = info;
  }

  public static class TargetNumber implements Serializable {

    private static final long serialVersionUID = -5556507267060252629L;

    private boolean noLimit;

    private Integer number;

    public boolean isNoLimit() {
      return noLimit;
    }

    public void setNoLimit(boolean noLimit) {
      this.noLimit = noLimit;
    }

    public Integer getNumber() {
      return number;
    }

    public void setNumber(Integer number) {
      this.number = number;
    }

  }

}
