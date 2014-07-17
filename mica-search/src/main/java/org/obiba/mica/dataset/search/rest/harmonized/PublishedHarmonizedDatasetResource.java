/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.harmonized;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizedDataset;
import org.obiba.mica.dataset.search.rest.PublishedDatasetSearchResource;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/harmonized-dataset/{id}")
@RequiresAuthentication
public class PublishedHarmonizedDatasetResource extends PublishedDatasetSearchResource<HarmonizedDataset> {

  @PathParam("id")
  private String id;

  /**
   * Get {@link org.obiba.mica.dataset.domain.HarmonizedDataset} from published index.
   *
   * @return
   */
  @GET
  public Mica.DatasetDto get() {
    return getDatasetDto(HarmonizedDataset.class, id);
  }

  /**
   * Get the {@link org.obiba.mica.dataset.domain.DatasetVariable}s from published index.
   * @return
   */
  @GET
  @Path("/variables")
  public List<Mica.DatasetVariableDto> getVariables() {
    return getDatasetVariableDtos(HarmonizedDataset.class, id);
  }

  @GET
  @Path("/variable/{name}")
  public Mica.DatasetVariableDto getVariable(@PathParam("name") String variableName) {
    return getDatasetVariableDto(HarmonizedDataset.class, id, variableName);
  }

  @Override
  protected DatasetVariable.Type getDatasetVariableType() {
    return DatasetVariable.Type.Dataschema;
  }

}
