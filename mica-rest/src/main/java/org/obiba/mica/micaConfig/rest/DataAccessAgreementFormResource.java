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
import org.obiba.mica.micaConfig.domain.DataAccessAgreementForm;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.DataAccessAgreementFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.Optional;

@Component
@Path("/config/data-access-agreement-form")
@RequiresAuthentication
public class DataAccessAgreementFormResource {

  private DataAccessConfigService dataAccessConfigService;
  private DataAccessAgreementFormService dataAccessAgreementFormService;

  private Dtos dtos;

  @Inject
  public DataAccessAgreementFormResource(DataAccessAgreementFormService dataAccessAgreementFormService,
                                         DataAccessConfigService dataAccessConfigService,
                                         Dtos dtos) {
    this.dataAccessAgreementFormService = dataAccessAgreementFormService;
    this.dataAccessConfigService = dataAccessConfigService;
    this.dtos = dtos;
  }

  @GET
  public Mica.DataAccessAgreementFormDto get(@QueryParam("revision") String revision) {
    Optional<DataAccessAgreementForm> d = dataAccessAgreementFormService.findByRevision(revision);
    if(!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    return dtos.asDto(d.get());
  }

  @PUT
  @Path("/_publish")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response publish() {
    dataAccessAgreementFormService.publish();
    return Response.ok().build();
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.DataAccessAgreementFormDto dto) {
    dataAccessAgreementFormService.createOrUpdate(dtos.fromDto(dto));
    return Response.ok().build();
  }

}
