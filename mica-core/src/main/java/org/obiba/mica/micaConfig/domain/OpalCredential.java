/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.domain;

import java.util.Arrays;
import java.util.Objects;

import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.micaConfig.AuthType;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class OpalCredential extends AbstractAuditableDocument {
  private static final long serialVersionUID = 1L;

  private String username;

  private String password;

  private AuthType authType;

  public OpalCredential() {
    super();
  }

  public OpalCredential(String opalUrl, AuthType authType) {
    super();
    setId(opalUrl);
    this.authType = authType;
  }

  public OpalCredential(String opalUrl, AuthType authType, String username, String password) {
    if (authType == AuthType.CERTIFICATE && (username != null || password != null)) {
      throw new IllegalArgumentException();
    } else if (authType == AuthType.USERNAME && (username == null || password == null)) {
      throw new IllegalArgumentException();
    }

    setId(opalUrl);
    this.username = username;
    this.password = password;
    this.authType = authType;
  }

  public String getOpalUrl() {
    return getId();
  }

  public void setOpalUrl(String opalUrl) {
    setId(opalUrl);
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public AuthType getAuthType() {
    return authType;
  }

  public void setAuthType(AuthType authType) {
    this.authType = authType;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] {getId(), getVersion(), authType, username, password});
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) return true;

    if(obj == null || getClass() != obj.getClass()) return false;

    return Objects.equals(getId(), ((OpalCredential) obj).getId()) &&
      getVersion() == ((OpalCredential) obj).getVersion() &&
      authType.equals(((OpalCredential) obj).getAuthType()) &&
      ((username == null && ((OpalCredential) obj).getUsername() == null) || username.equals(((OpalCredential) obj).getUsername())) &&
      ((password == null && ((OpalCredential) obj).getPassword() == null) || password.equals(((OpalCredential) obj).getPassword()));
  }
}
