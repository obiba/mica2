/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.security;

public interface Roles {

  // can edit/publish data and change system configuration
  String MICA_ADMIN = "mica-administrator";

  // can edit draft data and publish them
  String MICA_REVIEWER = "mica-reviewer";

  // can edit data draft data
  String MICA_EDITOR = "mica-editor";

  // can manage data access requests
  String MICA_DAO = "mica-data-access-officer";

  // can view published data
  String MICA_USER = "mica-user";
}
