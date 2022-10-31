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
import org.obiba.mica.micaConfig.domain.DataAccessPreliminaryForm;
import org.obiba.mica.micaConfig.service.DataAccessPreliminaryFormService;
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
@Path("/config/data-access-preliminary-form")
@RequiresAuthentication
public class DataAccessPreliminaryFormResource {

  private DataAccessPreliminaryFormService dataAccessPreliminaryFormService;

  private Dtos dtos;

  @Inject
  public DataAccessPreliminaryFormResource(DataAccessPreliminaryFormService dataAccessPreliminaryFormService,
                                           Dtos dtos) {
    this.dataAccessPreliminaryFormService = dataAccessPreliminaryFormService;
    this.dtos = dtos;
  }

  @GET
  public Mica.DataAccessPreliminaryFormDto get(@QueryParam("revision") String revision) {
    Optional<DataAccessPreliminaryForm> d = dataAccessPreliminaryFormService.findByRevision(revision);
    if(!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    return dtos.asDto(d.get());
  }

  @PUT
  @Path("/_publish")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response publish() {
    dataAccessPreliminaryFormService.publish();
    return Response.ok().build();
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.DataAccessPreliminaryFormDto dto) {
    dataAccessPreliminaryFormService.createOrUpdate(dtos.fromDto(dto));
    return Response.ok().build();
  }

}
