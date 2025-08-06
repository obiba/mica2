package org.obiba.mica.config;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;
import org.springframework.core.env.Environment;

import java.net.URI;

/**
 * If ?locale is missing, add it with the validated default (or "en").
 */
@Provider
@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
public class DefaultLocaleFilter implements ContainerRequestFilter {

  @Inject
  private Environment environment;

  @Override
  public void filter(ContainerRequestContext ctx) {

    if (ctx.getUriInfo().getQueryParameters().containsKey("locale")) {
      return;
    }

    String def = environment.getProperty(
      "locale.validatedLocale",
      environment.getProperty("locale.default", "en")
    );

    URI newUri = UriBuilder.fromUri(ctx.getUriInfo().getRequestUri())
      .replaceQueryParam("locale", def)
      .build();
    ctx.setRequestUri(newUri);
  }
}
