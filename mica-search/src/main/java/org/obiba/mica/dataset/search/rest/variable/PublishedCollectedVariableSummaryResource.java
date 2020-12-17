package org.obiba.mica.dataset.search.rest.variable;

import org.apache.shiro.SecurityUtils;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.HashMap;
import java.util.Map;

@Component
@Path("/")
@Scope("request")
public class PublishedCollectedVariableSummaryResource {

  private final CollectedDatasetService datasetService;
  private final MicaConfigService micaConfigService;
  private final Dtos dtos;

  @Inject
  public PublishedCollectedVariableSummaryResource(CollectedDatasetService datasetService, MicaConfigService micaConfigService, Dtos dtos) {
    this.datasetService = datasetService;
    this.micaConfigService = micaConfigService;
    this.dtos = dtos;
  }

  @GET
  @Path("/collected/{project}/{table}/{variableName}")
  public DatasetVariable getVariable(@PathParam("project") String project, @PathParam("table") String table, @PathParam("variableName") String variableName) {
    checkVariableSummaryAccess();
    return datasetService.getDatasetVariable(project, table, variableName);
  }

  @GET
  @Path("/collected/{project}/{table}/{variableName}/_summary")
  public Mica.DatasetVariableAggregationDto getVariableSummary(@PathParam("project") String project, @PathParam("table") String table, @PathParam("variableName") String variableName) {
    checkVariableSummaryAccess();
    return dtos.asDto(datasetService.getVariableSummary(project, table, variableName).getWrappedDto()).build();
  }

  private void checkVariableSummaryAccess() {
    if (!SecurityUtils.getSubject().isAuthenticated() && micaConfigService.getConfig().isVariableSummaryRequiresAuthentication())
      throw new ForbiddenException();
  }
}
