package org.obiba.mica.web.controller.domain;

import com.google.common.base.Strings;
import org.obiba.oidc.OIDCAuthProviderSummary;

public class OidcProvider {

  private final OIDCAuthProviderSummary authProviderSummary;

  private final String signinUrl;

  public OidcProvider(OIDCAuthProviderSummary authProviderSummary, String signinUrl) {
    this.authProviderSummary = authProviderSummary;
    this.signinUrl = signinUrl;
  }

  public String getName() {
    return authProviderSummary.getName();
  }

  public String getTitle() {
    String title = authProviderSummary.getTitle();
    return Strings.isNullOrEmpty(title) ? getName() : title;
  }

  public String getSigninUrl() {
    return signinUrl;
  }

  public String getAccountUrl() {
    return authProviderSummary.getProviderUrl();
  }

}
