package org.obiba.mica.dataset.rest;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

@Component
@Scope("request")
@Path("/draft/datasets")
public class DraftDatasetsResource {

  @Inject
  private EventBus eventBus;

  @PUT
  @Path("/_index")
  @RequiresPermissions({ "/draft/study-dataset:EDIT", "/draft/harmonization-dataset:EDIT" })
  public Response indexAll() {
    eventBus.post(new IndexDatasetsEvent());
    return Response.noContent().build();
  }
}
