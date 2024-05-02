package org.obiba.mica.web.rest.security;

import org.obiba.mica.core.service.UserAuthService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.web.model.OIDCDtos;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("request")
@Path("/auth/providers")
public class OidcProvidersResource {


  private final Dtos dtos;

  private final UserAuthService userAuthService;

  @Inject
  public OidcProvidersResource(Dtos dtos, UserAuthService userAuthService) {
    this.dtos = dtos;
    this.userAuthService = userAuthService;
  }

  @GET
  public List<OIDCDtos.OIDCAuthProviderSummaryDto> getProviders(@Nullable @QueryParam("locale") @DefaultValue("en") String locale, @QueryParam("signup") @DefaultValue("false") boolean signupOnly) {
    return userAuthService.getOidcProviders(locale, signupOnly)
      .stream()
      .map(dtos::asSummaryDto)
      .collect(Collectors.toList());
  }
}
