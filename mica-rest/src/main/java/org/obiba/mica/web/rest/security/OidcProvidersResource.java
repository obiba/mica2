package org.obiba.mica.web.rest.security;

import org.obiba.mica.core.service.OidcProvidersService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.web.model.OIDCDtos;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("request")
@Path("/auth/providers")
public class OidcProvidersResource {


  private final Dtos dtos;

  private final OidcProvidersService oidcProvidersService;

  @Inject
  public OidcProvidersResource(Dtos dtos, OidcProvidersService oidcProvidersService) {
    this.dtos = dtos;
    this.oidcProvidersService = oidcProvidersService;
  }

  @GET
  public List<OIDCDtos.OIDCAuthProviderSummaryDto> getProviders(@Nullable @QueryParam("locale") @DefaultValue("en") String locale) {
    return oidcProvidersService.getProviders(locale)
      .stream()
      .map(dtos::asSummaryDto)
      .collect(Collectors.toList());
  }
}
