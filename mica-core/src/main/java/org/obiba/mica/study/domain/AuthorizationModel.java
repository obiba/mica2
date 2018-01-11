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

import org.obiba.mica.core.domain.Authorization;

import java.io.Serializable;
import java.text.SimpleDateFormat;

public class AuthorizationModel implements Serializable {

  private static final long serialVersionUID = -3098622168836971902L;

  private boolean authorized;
  private String authorizer;
  private String date;

  public AuthorizationModel() {
  }

  public AuthorizationModel(Authorization authorization) {
    this.authorized = authorization.isAuthorized();
    this.authorizer = authorization.getAuthorizer();
    if (authorization.getDate() != null)
      this.date = new SimpleDateFormat("yyyy-MM-dd").format(authorization.getDate());
  }

  public boolean isAuthorized() {
    return authorized;
  }

  public void setAuthorized(boolean authorized) {
    this.authorized = authorized;
  }

  public String getAuthorizer() {
    return authorizer;
  }

  public void setAuthorizer(String authorizer) {
    this.authorizer = authorizer;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }
}
