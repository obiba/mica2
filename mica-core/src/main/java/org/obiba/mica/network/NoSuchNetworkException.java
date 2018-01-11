/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network;

import java.util.NoSuchElementException;

public class NoSuchNetworkException extends NoSuchElementException {

  private static final long serialVersionUID = 5931123739037832740L;

  private NoSuchNetworkException(String s) {
    super(s);
  }

  public static NoSuchNetworkException withId(String id) {
    return new NoSuchNetworkException("Network with id '" + id + "' does not exist");
  }

}
