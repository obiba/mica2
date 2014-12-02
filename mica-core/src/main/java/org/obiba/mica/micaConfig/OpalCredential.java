package org.obiba.mica.micaConfig;

import java.util.Arrays;
import java.util.Objects;

import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class OpalCredential extends AbstractAuditableDocument {
  private static final long serialVersionUID = 1L;

  private String username;

  private String password;

  private AuthType authType;

  protected OpalCredential() {
    super();
  }

  public OpalCredential(String opalUrl, AuthType authType) {
    super();
    setId(opalUrl);
    this.authType = authType;
  }

  public OpalCredential(String opalUrl, AuthType authType, String username, String password) {
    super();

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
