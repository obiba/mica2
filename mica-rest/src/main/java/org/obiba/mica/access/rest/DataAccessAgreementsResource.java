/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.rest;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.access.domain.DataAccessAgreement;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessAgreementService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.Mica.DataAccessRequestDto.StatusChangeDto;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("request")
@RequiresAuthentication
public class DataAccessAgreementsResource {

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private DataAccessAgreementService dataAccessAgreementService;

  private String parentId;

  @POST
  @Timed
  public Response create(Mica.DataAccessRequestDto dto, @Context UriInfo uriInfo) {
    DataAccessAgreement agreement = dtos.fromAgreementDto(dto);
    return saveNew(agreement, uriInfo);
  }

  @POST
  @Path("/_empty")
  public Response create(@Context UriInfo uriInfo) {
    DataAccessAgreement agreement = new DataAccessAgreement();
    agreement.setContent("{}");
    return saveNew(agreement, uriInfo);
  }

  private Response saveNew(DataAccessAgreement agreement, UriInfo uriInfo) {
    String resource = String.format("/data-access-request/%s/agreements", parentId);
    subjectAclService.checkPermission(resource, "ADD");
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    // force applicant (could be different from the request's applicant)
    String applicant = SecurityUtils.getSubject().getPrincipal().toString();
    agreement.setApplicant(applicant);
    agreement.setId(null);
    agreement.setParentId(parentId);
    agreement.setStatus(DataAccessEntityStatus.OPENED);

    // set permissions
    dataAccessAgreementService.save(agreement);
    resource = String.format("/data-access-request/%s/agreement", parentId);
    subjectAclService.addUserPermission(applicant, resource, "VIEW,EDIT,DELETE", agreement.getId());
    subjectAclService.addUserPermission(applicant,resource + "/" + agreement.getId(), "EDIT", "_status");

    return Response.created(uriInfo.getBaseUriBuilder().segment("data-access-request", parentId, "agreement", agreement.getId()).build()).build();
  }

  @GET
  @Timed
  public List<Mica.DataAccessRequestDto> listByStatus(@QueryParam("status") List<String> status) {
    return listByStatusFilteringPermitted(status).stream()
      .map(dtos::asAgreementDto)
      .collect(Collectors.toList());
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @GET
  @Path("/_history")
  public List<StatusChangeDto> getLoggedHistory() {
    List<StatusChangeDto> statusChangeDtos = new ArrayList<>();
    dataAccessAgreementService.findByParentId(parentId).forEach(agreement ->
      statusChangeDtos.addAll(dtos.asStatusChangeDtoList(agreement))
    );

    return statusChangeDtos;
  }

  private List<DataAccessAgreement> listByStatusFilteringPermitted(List<String> status) {
    String resource = "/data-access-request";
    List<DataAccessAgreement> agreements = dataAccessAgreementService.findByStatus(parentId, status);
    return agreements.stream() //
      .filter(agreement -> subjectAclService.isPermitted(resource, "VIEW", parentId)) //
      .collect(Collectors.toList());
  }
}
