/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.rest;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.joda.time.DateTime;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("request")
@Path("/data-access-requests")
public class DataAccessRequestsResource {

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private Dtos dtos;

  @Inject
  private DataAccessFormService dataAccessFormService;

  @GET
  @Timed
  public List<Mica.DataAccessRequestDto> listByStatus(@QueryParam("status") List<String> status) {
    return listByStatusFilteringPermitted(status).stream()
      .map(dtos::asDto)
      .collect(Collectors.toList());
  }

  @GET
  @Timed
  @Path("/csv")
  @Produces("text/csv")
  public Response exportCsv(@QueryParam("lang") String lang) {

    List<DataAccessRequest> dataAccessRequests = listByStatusFilteringPermitted(null);
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    new CsvReportGenerator(dataAccessRequests, dataAccessForm.getCsvExportFormat(), lang).write(byteArrayOutputStream);

    String date = new DateTime().toString("YYYY-MM-dd");
    return Response.ok(byteArrayOutputStream.toByteArray()).header("Content-Disposition", String.format("attachment; filename=\"Access-Requests-Report_%s.csv\"", date)).build();
  }

  @GET
  @Path("/applicant/{applicant}")
  @Timed
  public List<Mica.DataAccessRequestDto> list(@PathParam("applicant") String applicant) {
    return dataAccessRequestService.findAll(applicant).stream() //
      .filter(req -> subjectAclService.isPermitted("/data-access-request", "VIEW", req.getId())) //
      .map(dtos::asDto).collect(Collectors.toList());
  }

  @POST
  @Timed
  @RequiresPermissions("/data-access-request:ADD")
  public Response create(Mica.DataAccessRequestDto dto, @Context UriInfo uriInfo) {
    DataAccessRequest request = dtos.fromDto(dto);

    // force applicant and make sure it is a new request
    String applicant = SecurityUtils.getSubject().getPrincipal().toString();
    request.setApplicant(applicant);
    request.setId(null);
    request.setStatus(DataAccessRequest.Status.OPENED);

    dataAccessRequestService.save(request);

    subjectAclService.addPermission("/data-access-request", "VIEW,EDIT,DELETE", request.getId());
    subjectAclService.addPermission("/data-access-request/" + request.getId(), "EDIT", "_status");
    subjectAclService.addPermission("/data-access-request/" + request.getId() + "/_attachments", "EDIT");
    subjectAclService.addGroupPermission(Roles.MICA_DAO, "/data-access-request/" + request.getId() + "/_attachments", "EDIT", null);

    return Response.created(uriInfo.getBaseUriBuilder().segment("data-access-request", request.getId()).build()).build();
  }

  private List<DataAccessRequest> listByStatusFilteringPermitted(List<String> status) {
    List<DataAccessRequest> reqs = dataAccessRequestService.findByStatus(status);
    return reqs.stream() //
      .filter(req -> subjectAclService.isPermitted("/data-access-request", "VIEW", req.getId())) //
      .collect(Collectors.toList());
  }
}
