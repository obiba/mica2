/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.NoSuchDataAccessFormException;
import org.obiba.mica.micaConfig.domain.DataAccessFeasibilityForm;
import org.obiba.mica.micaConfig.service.DataAccessFeasibilityFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Component
@Path("/config/data-access-feasibility-form")
@RequiresAuthentication
public class DataAccessFeasibilityFormResource {

  private DataAccessFeasibilityFormService dataAccessFeasibilityFormService;

  private Dtos dtos;

  @Inject
  public DataAccessFeasibilityFormResource(DataAccessFeasibilityFormService dataAccessFeasibilityFormService,
                                           Dtos dtos) {
    this.dataAccessFeasibilityFormService = dataAccessFeasibilityFormService;
    this.dtos = dtos;
  }

  @GET
  public Mica.DataAccessFeasibilityFormDto get(@QueryParam("revision") String revision) {
    Optional<DataAccessFeasibilityForm> d = dataAccessFeasibilityFormService.findByRevision(revision);
    if(!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    return dtos.asDto(d.get());
  }

  @PUT
  @Path("/_publish")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response publish() {
    dataAccessFeasibilityFormService.publish();
    return Response.ok().build();
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.DataAccessFeasibilityFormDto dto) {
    dataAccessFeasibilityFormService.createOrUpdate(dtos.fromDto(dto));
    return Response.ok().build();
  }

}
