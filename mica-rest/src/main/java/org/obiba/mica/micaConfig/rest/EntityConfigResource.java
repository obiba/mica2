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

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.obiba.mica.micaConfig.service.EntityConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Optional;

@Component
public abstract class EntityConfigResource<T extends EntityConfig, U> {

  @Inject
  ApplicationContext applicationContext;

  @Inject
  Dtos dtos;

  @Inject
  EntityConfigTranslator entityConfigTranslator;

  protected abstract U asDto(T entityConfig);

  protected abstract T fromDto(U entityConfig);

  @GET
  @Path("/form")
  public U get(@Context UriInfo uriInfo, @QueryParam("locale") String locale) {

    Optional<T> optionalConfig = getConfigService().findComplete();

    if (!optionalConfig.isPresent())
      throw NoSuchEntityException.withPath(EntityConfig.class, uriInfo.getPath());

    T config = optionalConfig.get();
    entityConfigTranslator.translateSchemaForm(locale, config);

    return asDto(config);
  }

  @GET
  @Path("/form-custom")
  public U getComplete(@Context UriInfo uriInfo) {

    Optional<T> optionalConfig = getConfigService().findPartial();

    if (!optionalConfig.isPresent())
      throw NoSuchEntityException.withPath(EntityConfig.class, uriInfo.getPath());

    return asDto(optionalConfig.get());
  }

  @PUT
  @Path("/form-custom")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(U dto) {
    getConfigService().createOrUpdate(fromDto(dto));
    return Response.ok().build();
  }

  protected abstract EntityConfigService<T> getConfigService();

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }
}
