/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

@Component
public class CustomTranslationsResource {

  private static final Logger logger = LoggerFactory.getLogger(CustomTranslationsResource.class);

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private ObjectMapper objectMapper;

  @GET
  @Timed
  @Path("/{locale}.json")
  @Produces("application/json")
  public Response getCustomTranslation(@PathParam("locale") String locale) {
    return Response.ok(getTranslations(locale).toString()).build();
  }

  @GET
  @Timed
  @Path("/export")
  @Produces("application/json")
  public Response exportTranslation() {
    List<String> locales = micaConfigService.getConfig().getLocalesAsString();
    ObjectNode node = objectMapper.createObjectNode();
    locales.forEach(l -> node.set(l, getTranslations(l)));

    return Response.ok(node.toString()).build();
  }

  @PUT
  @Path("/{locale}.json")
  @Consumes("application/json")
  public Response save(String translations, @PathParam("locale") String locale,
                       @QueryParam("merge") @DefaultValue("false") boolean merge) throws IOException {
    MicaConfig config = micaConfigService.getConfig();

    if (merge) {
      micaConfigService.mergeJson(getTranslations(locale), objectMapper.readTree(translations));
    } else {
      config.getTranslations().put(locale, translations);
    }

    micaConfigService.save(config);
    return Response.ok().build();
  }

  @PUT
  @Path("/import")
  @Consumes("application/json")
  public Response importTranslations(String translations,
                                     @QueryParam("merge") @DefaultValue("false") boolean merge) throws IOException {
    MicaConfig config = micaConfigService.getConfig();
    JsonNode node = objectMapper.readTree(translations);
    List<String> locales = config.getLocalesAsString();

    if (!config.hasTranslations()) {
      config.setTranslations(new LocalizedString());
    }

    if (merge) {
      locales.forEach(l -> {
        JsonNode merged = micaConfigService.mergeJson(getTranslations(l), node.get(l));
        config.getTranslations().put(l, merged.toString());
      });
    } else {
      locales.forEach(l -> config.getTranslations().put(l, node.get(l).toString()));
    }

    micaConfigService.save(config);

    return Response.ok().build();
  }

  private JsonNode getTranslations(@NotNull String locale) {
    MicaConfig config = micaConfigService.getConfig();
    JsonNode node = objectMapper.createObjectNode();

    if (config.hasTranslations() && config.getTranslations().get(locale) != null) {
      try {
        node = objectMapper.readTree(config.getTranslations().get(locale));
      } catch (IOException e) {
        logger.warn("Cannot read custom translations tree for locale {0}", locale, e);
      }
    }

    return node;
  }
}
