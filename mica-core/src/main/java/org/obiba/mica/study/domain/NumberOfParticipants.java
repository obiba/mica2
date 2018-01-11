/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.domain;

import java.io.Serializable;

import org.obiba.mica.core.domain.LocalizedString;

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
