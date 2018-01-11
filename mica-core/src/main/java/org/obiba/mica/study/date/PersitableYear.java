/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.date;

import java.io.Serializable;

public class PersitableYear implements Serializable {

  private static final long serialVersionUID = -6435738035809115928L;

  private static final YearValidator VALIDATOR = new YearValidator();

  private int year;

  public void setYear(int value) {
    VALIDATOR.validate(value);
    year = value;
  }

  public int getYear() {
    return year;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof PersitableYear) {
      PersitableYear other = (PersitableYear) obj;
      return year == other.year;
    }
    return false;
  }

}
