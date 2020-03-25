package org.obiba.mica.web.controller.domain;

import com.google.common.base.Strings;
import org.obiba.oidc.OIDCAuthProviderSummary;

public class OidcProvider {

  private final OIDCAuthProviderSummary authProviderSummary;

  private final String url;

  public OidcProvider(OIDCAuthProviderSummary authProviderSummary, String url) {
    this.authProviderSummary = authProviderSummary;
    this.url = url;
  }

  public String getName() {
    return authProviderSummary.getName();
  }

  public String getTitle() {
    String title = authProviderSummary.getTitle();
    return Strings.isNullOrEmpty(title) ? getName() : title;
  }

  public String getUrl() {
    return url;
  }

  public String getAccountUrl() {
    return authProviderSummary.getProviderUrl();
  }

}
