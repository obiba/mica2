/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.security;

public enum Roles {
  MICA_ADMIN {
    @Override
    public String toString() {
      return "mica-administrator";
    }
  }, //
  MICA_USER {
    @Override
    public String toString() {
      return "mica-user";
    }
  }
}
