package org.obiba.mica.web.model;

import org.obiba.oidc.OIDCAuthProviderSummary;
import org.obiba.web.model.OIDCDtos;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Component
public class OidcAuthProviderSummaryDtos {

  @NotNull
  public OIDCDtos.OIDCAuthProviderSummaryDto asSummaryDto(@NotNull OIDCAuthProviderSummary summary) {
    OIDCDtos.OIDCAuthProviderSummaryDto.Builder builder = OIDCDtos.OIDCAuthProviderSummaryDto.newBuilder()
      .setName(summary.getName())
      .setProviderUrl(summary.getProviderUrl());

    Optional.ofNullable(summary.getTitle()).ifPresent(builder::setTitle);
    return builder.build();
  }
}
