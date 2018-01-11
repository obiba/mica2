/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access;

import java.util.NoSuchElementException;

import com.google.common.base.Strings;

public class NoSuchDataAccessRequestException extends NoSuchElementException {

  private static final long serialVersionUID = 2056200745616902456L;

  private final String requestId;

  private NoSuchDataAccessRequestException(String s, String id)
  {
    super(s);
    requestId = id;
  }

  public static NoSuchDataAccessRequestException withId(String id) {
    return new NoSuchDataAccessRequestException("Data access request with id '" + id + "' does not exist", id);
  }

  public boolean hasRequestId() {
    return !Strings.isNullOrEmpty(requestId);
  }

  public String getRequestId() {
    return requestId;
  }
}
