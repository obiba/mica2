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

public class YearValidator implements Validator {
  @Override
  public void validate(int value) {
    if (value < 1900) throw new IllegalArgumentException("Year must be greater than 1900");

    if (value > 9999) throw new IllegalArgumentException("Year must be less or equal than 9999");
  }
}
