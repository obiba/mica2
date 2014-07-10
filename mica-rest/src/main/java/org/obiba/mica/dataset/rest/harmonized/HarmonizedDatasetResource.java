/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.harmonized;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.HarmonizedDataset;
import org.obiba.mica.service.HarmonizedDatasetService;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Magma;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

@Component
@Scope("request")
@RequiresAuthentication
public class HarmonizedDatasetResource {

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private HarmonizedDatasetService datasetService;

  @Inject
  private org.obiba.mica.web.model.Dtos dtos;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  public Mica.DatasetDto get() {
    return dtos.asDto(datasetService.findById(id));
  }

  @PUT
  public Response index() {
    datasetService.index(id);
    return Response.noContent().build();
  }

  @GET
  @Path("/table")
  public Magma.TableDto getTable() {
    return datasetService.getTableDto(getDataset());
  }

  @GET
  @Path("/variables")
  public List<Mica.DatasetVariableDto> getVariables() {
    ImmutableList.Builder<Mica.DatasetVariableDto> builder = ImmutableList.builder();
    datasetService.getDatasetVariables(getDataset()).forEach(variable -> builder.add(dtos.asDto(variable)));
    return builder.build();
  }

  @Path("/variable/{variable}")
  public DataschemaDatasetVariableResource getVariable(@PathParam("variable") String variable) {
    DataschemaDatasetVariableResource resource = applicationContext.getBean(DataschemaDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setName(variable);
    return resource;
  }

  private HarmonizedDataset getDataset() {
    return datasetService.findById(id);
  }

}
