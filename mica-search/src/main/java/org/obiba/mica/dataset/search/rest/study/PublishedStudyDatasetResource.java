/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.study;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.service.StudyDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Study variable resource: variable describing a study dataset.
 */
@Component
@Scope("request")
@Path("/study-dataset/{id}")
@RequiresAuthentication
public class PublishedStudyDatasetResource extends AbstractPublishedDatasetResource<StudyDataset> {

  @PathParam("id")
  private String id;

  @Inject
  private StudyDatasetService datasetService;

  @Inject
  private Dtos dtos;

  /**
   * Get {@link org.obiba.mica.dataset.domain.StudyDataset} from published index.
   *
   * @return
   */
  @GET
  public Mica.DatasetDto get() {
    return getDatasetDto(StudyDataset.class, id);
  }

  /**
   * Get the {@link org.obiba.mica.dataset.domain.DatasetVariable}s from published index.
   *
   * @return
   */
  @GET
  @Path("/variables")
  public List<Mica.DatasetVariableDto> getVariables() {
    return getDatasetVariableDtos(StudyDataset.class, id);
  }

  @Path("/variable/{variable}")
  public PublishedStudyDatasetVariableResource getVariable(@PathParam("variable") String variable) {
    PublishedStudyDatasetVariableResource resource = applicationContext.getBean(PublishedStudyDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    return resource;
  }

  @POST
  @Path("/facets")
  public Search.QueryResultDto getFacets(Search.QueryTermsDto query) {
    return datasetService.getFacets(getDataset(StudyDataset.class, id), query);
  }

  @Override
  protected DatasetVariable.Type getDatasetVariableType() {
    return DatasetVariable.Type.Study;
  }
}
