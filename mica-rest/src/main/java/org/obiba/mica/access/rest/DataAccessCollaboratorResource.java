package org.obiba.mica.access.rest;


import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.access.domain.DataAccessCollaborator;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessCollaboratorService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.util.Optional;

@Component
@Scope("request")
@RequiresAuthentication
public class DataAccessCollaboratorResource {

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

  @Inject
  private UserProfileService userProfileService;

  private String email;

  private String parentId;

  public void setEmail(String email) {
    this.email = email;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @GET
  public Mica.DataAccessCollaboratorDto getCollaborator() {
    subjectAclService.checkPermission("/data-access-request", "VIEW", parentId);
    Optional<DataAccessCollaborator> collaboratorOpt = dataAccessCollaboratorService.findByRequestIdAndEmail(parentId, email);
    return dtos.asDto(collaboratorOpt.orElseThrow(NotFoundException::new));
  }

  @DELETE
  public Response deleteCollaborator() {
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (!subjectAclService.isCurrentUser(request.getApplicant()))
      subjectAclService.checkPermission("/data-access-request", "EDIT", parentId);
    Optional<DataAccessCollaborator> collaboratorOpt = dataAccessCollaboratorService.findByRequestIdAndEmail(parentId, email);
    collaboratorOpt.ifPresent(collaborator -> dataAccessCollaboratorService.delete(collaborator));
    return Response.ok().build();
  }
}
