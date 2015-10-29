package org.obiba.mica.dataset.rest;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/datasets")
public class DatasetsResource {

  @Inject
  Helper helper;

  @PUT
  @Path("/_index")
  @RequiresPermissions({ "/draft/study-dataset:EDIT", "/draft/harmonization-dataset:EDIT" })
  public Response indexAll() {
    helper.indexAll();

    return Response.ok().build();
  }

  @Component
  public static class Helper {

    @Inject
    private StudyDatasetService studyDatasetService;

    @Inject
    private HarmonizationDatasetService harmonizationDatasetService;

    @Async
    public void indexAll() {
      studyDatasetService.indexAll();
      harmonizationDatasetService.indexAll();
    }
  }
}
