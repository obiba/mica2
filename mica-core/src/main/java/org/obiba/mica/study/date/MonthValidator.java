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

public class MonthValidator implements Validator {
  @Override
  public void validate(int value) {
    if (value < 1 || value > 12 ) throw new IllegalArgumentException("Month must be between 1 and 12");
  }
}
