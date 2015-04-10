package org.obiba.mica.dataset.rest;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.service.DatasetIndexer;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.dataset.service.VariableIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@RequiresAuthentication
@Path("/datasets")
public class DatasetsResource {

  @Inject
  Helper helper;

  @PUT
  @Path("/_index")
  public Response indexAll() {
    helper.indexAll();

    return Response.ok().build();
  }

  @Component
  public static class Helper {
    private static final Logger log = LoggerFactory.getLogger(DatasetsResource.Helper.class);

    @Inject
    private DatasetIndexer datasetDatasetIndexer;

    @Inject
    private VariableIndexer variableIndexer;

    @Inject
    private StudyDatasetService studyDatasetService;

    @Inject
    private HarmonizationDatasetService harmonizationDatasetService;

    @Async
    public void indexAll() {
      log.info("Reindexing datasets");

      datasetDatasetIndexer.dropIndex();
      variableIndexer.dropIndex();

      studyDatasetService.indexAll(true);
      harmonizationDatasetService.indexAll(true);
    }
  }
}
