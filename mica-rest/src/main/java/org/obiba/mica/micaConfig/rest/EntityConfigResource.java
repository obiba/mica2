package org.obiba.mica.micaConfig.rest;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.obiba.mica.micaConfig.service.EntityConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public abstract class EntityConfigResource<T extends EntityConfig> {

  @Inject
  ApplicationContext applicationContext;

  @Inject
  Dtos dtos;

  protected abstract Mica.EntityFormDto asDto(T entityConfig);

  protected abstract T fromDto(Mica.EntityFormDto entityConfig);

  @GET
  @Path("/form")
  public Mica.EntityFormDto get(@Context UriInfo uriInfo) {
    Optional<T> d = getConfigService().find();
    if(!d.isPresent()) throw NoSuchEntityException.withPath(EntityConfig.class, uriInfo.getPath());
    return asDto(d.get());
  }

  @PUT
  @Path("/form")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.EntityFormDto dto) {
    getConfigService().createOrUpdate(fromDto(dto));
    return Response.ok().build();
  }

  protected abstract EntityConfigService<T> getConfigService();
}
