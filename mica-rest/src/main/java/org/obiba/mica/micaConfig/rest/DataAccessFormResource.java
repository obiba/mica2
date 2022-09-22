/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
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
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.rest.SubjectAclResource;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Component
@Path("/config/data-access-form")
@RequiresAuthentication
public class DataAccessFormResource {

  @Inject
  DataAccessFormService dataAccessFormService;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  Dtos dtos;

  @GET
  public Mica.DataAccessFormDto get(@QueryParam("revision") String revision) {
    Optional<DataAccessForm> d = dataAccessFormService.findByRevision(revision);
    if(!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    return dtos.asDto(d.get());
  }

  @PUT
  @Path("/_publish")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response publish() {
    dataAccessFormService.publish();
    return Response.ok().build();
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.DataAccessFormDto dto) {
    dataAccessFormService.createOrUpdate(dtos.fromDto(dto));
    return Response.ok().build();
  }

  @Path("/permissions")
  @RequiresRoles(Roles.MICA_ADMIN)
  public SubjectAclResource permissions() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/data-access-request", "*");
    subjectAclResource.addOtherResourceName("action-logs");
    subjectAclResource.addOtherResourceName("private-comment");
    subjectAclResource.setFileResourceInstance("/file", "/data-access-request");
    return subjectAclResource;
  }
}
