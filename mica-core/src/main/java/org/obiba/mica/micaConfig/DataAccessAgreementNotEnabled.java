/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig;

public class DataAccessAgreementNotEnabled extends RuntimeException {

  private static final long serialVersionUID = -133123777779793L;

  public DataAccessAgreementNotEnabled() {
    super("Data Access end user agreement capability is not enabled.");
  }

}
