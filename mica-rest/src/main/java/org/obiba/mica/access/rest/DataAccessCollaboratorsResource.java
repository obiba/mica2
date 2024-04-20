package org.obiba.mica.access.rest;


import com.google.common.base.Strings;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessCollaboratorService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("request")
@RequiresAuthentication
public class DataAccessCollaboratorsResource {

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private DataAccessCollaboratorService dataAccessCollaboratorService;

  private String parentId;

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @GET
  public List<Mica.DataAccessCollaboratorDto> getCollaborators() {
    subjectAclService.checkPermission("/data-access-request", "VIEW", parentId);
    return dataAccessCollaboratorService.findByRequestId(parentId).stream()
      .map(dtos::asDto)
      .collect(Collectors.toList());
  }

  @GET
  @Path("/_suggest")
  public List<String> suggestCollaborators() {
    subjectAclService.checkPermission("/data-access-request", "VIEW", parentId);
    DataAccessRequest dar = dataAccessRequestService.findById(parentId);
    return dataAccessRequestUtilService.getEmails(dar);
  }

  @PUT
  @Path("/_invite")
  public Response inviteCollaborator(@QueryParam("email") String email) {
    if (!dataAccessRequestUtilService.getDataAccessConfig().isCollaboratorsEnabled())
      return Response.status(Response.Status.FORBIDDEN).build();
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (!subjectAclService.isCurrentUser(request.getApplicant()))
      subjectAclService.checkPermission("/data-access-request", "EDIT", parentId);
    if (Strings.isNullOrEmpty(email) || !email.contains("@")) throw new BadRequestException("Not a valid email");

    dataAccessCollaboratorService.inviteCollaborator(request, email);
    return Response.ok().build();
  }

}
