/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.apache.shiro.SecurityUtils;
import org.obiba.mica.core.source.OpalTableSource;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing Dataset.
 */
@Component
@Path("/dataset/{id}")
@Scope("request")
public class PublishedDatasetResource {

  private final PublishedDatasetService publishedDatasetService;

  private final CollectedDatasetService collectedDatasetService;

  private final MicaConfigService micaConfigService;

  private final Dtos dtos;

  @Inject
  public PublishedDatasetResource(PublishedDatasetService publishedDatasetService, CollectedDatasetService collectedDatasetService, MicaConfigService micaConfigService, Dtos dtos) {
    this.publishedDatasetService = publishedDatasetService;
    this.collectedDatasetService = collectedDatasetService;
    this.micaConfigService = micaConfigService;
    this.dtos = dtos;
  }

  @GET
  @Timed
  public Mica.DatasetDto get(@PathParam("id") String id) {
    Dataset dataset = getDataset(id);
    return dtos.asDto(dataset);
  }

  @GET
  @Path("/collected/{project}/{table}/{variableName}")
  public DatasetVariable getVariable(@PathParam("id") String id, @PathParam("project") String project, @PathParam("table") String table, @PathParam("variableName") String variableName) {
    checkVariableSummaryAccess();
    return collectedDatasetService.getDatasetVariable(alternativeStudyDataset(id, project, table), variableName);
  }

  @GET
  @Path("/collected/{project}/{table}/{variableName}/_summary")
  public Mica.DatasetVariableAggregationDto getVariableSummary(@PathParam("id") String id, @PathParam("project") String project, @PathParam("table") String table, @PathParam("variableName") String variableName) {
    checkVariableSummaryAccess();
    Mica.DatasetVariableAggregationDto summary = collectedDatasetService.getVariableSummary(alternativeStudyDataset(id, project, table), variableName);
    return collectedDatasetService.getFilteredVariableSummary(summary);
  }

  private Dataset getDataset(String id) {
    Dataset dataset = publishedDatasetService.findById(id);
    if (dataset == null) throw NoSuchDatasetException.withId(id);

    return dataset;
  }

  private StudyDataset alternativeStudyDataset(String id, String project, String table) {
    Dataset dataset = getDataset(id);
    if (!(dataset instanceof StudyDataset)) throw NoSuchDatasetException.withId(id);

    StudyDataset asStudyDataset = (StudyDataset) dataset;
    asStudyDataset.getStudyTable().setSource(OpalTableSource.newSource(project, table).getURN());

    return asStudyDataset;
  }

  private void checkVariableSummaryAccess() {
    if (!SecurityUtils.getSubject().isAuthenticated() && micaConfigService.getConfig().isVariableSummaryRequiresAuthentication())
      throw new ForbiddenException();
  }

}
