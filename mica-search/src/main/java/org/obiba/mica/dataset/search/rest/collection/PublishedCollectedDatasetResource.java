/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.collection;

import com.codahale.metrics.annotation.Timed;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;

/**
 * Study variable resource: variable describing a collection dataset.
 */
@Component
@Scope("request")
@Path("/collected-dataset/{id}")
public class PublishedCollectedDatasetResource extends AbstractPublishedDatasetResource<StudyDataset> {

  /**
   * Get {@link StudyDataset} from published index.
   *
   * @return
   */
  @GET
  @Timed
  public Mica.DatasetDto get(@PathParam("id") String id) {
    checkDatasetAccess(id);
    return getDatasetDto(StudyDataset.class, id);
  }

  /**
   * Get the {@link DatasetVariable}s from published index.
   *
   * @param queryString - Elasticsearch query string 'field1: value AND field2: value'
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @return
   */
  @GET
  @Path("/variables/_search")
  @Timed
  public Mica.DatasetVariablesDto queryVariables(@PathParam("id") String id, @QueryParam("query") String queryString,
                                                 @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
                                                 @QueryParam("sort") String sort, @QueryParam("order") String order) {
    checkDatasetAccess(id);
    return getDatasetVariableDtos(queryString, id, DatasetVariable.Type.Collected, from, limit, sort, order);
  }

  /**
   * Get the {@link DatasetVariable}s from published index.
   *
   * @return
   */
  @GET
  @Path("/variables")
  @Timed
  public Mica.DatasetVariablesDto getVariables(@PathParam("id") String id,
                                               @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
                                               @QueryParam("sort") String sort, @QueryParam("order") String order) {
    checkDatasetAccess(id);
    return getDatasetVariableDtos(id, DatasetVariable.Type.Collected, from, limit, sort, order);
  }

  @Path("/variable/{variable}")
  public PublishedCollectedDatasetVariableResource getVariable(@PathParam("id") String id,
                                                               @PathParam("variable") String variable) {
    checkDatasetAccess(id);
    PublishedCollectedDatasetVariableResource resource = applicationContext
      .getBean(PublishedCollectedDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    return resource;
  }

  private void checkDatasetAccess(String id) {
    subjectAclService.checkAccess("/collected-dataset", id);
  }
}
