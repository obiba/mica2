package org.obiba.mica.domain.study;

import java.io.Serializable;

import org.obiba.mica.domain.LocalizedString;

public class NumberOfParticipants implements Serializable {

  private static final long serialVersionUID = -9146572889284008519L;

  private TargetNumber participant;

  private TargetNumber sample;

  private LocalizedString infos;

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

  public LocalizedString getInfos() {
    return infos;
  }

  public void setInfos(LocalizedString infos) {
    this.infos = infos;
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
