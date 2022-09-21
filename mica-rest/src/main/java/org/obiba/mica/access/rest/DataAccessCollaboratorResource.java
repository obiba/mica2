package org.obiba.mica.access.rest;


import com.google.common.base.Strings;
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

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    subjectAclService.checkPermission("/data-access-request", "EDIT", parentId);
    Optional<DataAccessCollaborator> collaboratorOpt = dataAccessCollaboratorService.findByRequestIdAndEmail(parentId, email);
    collaboratorOpt.ifPresent(collaborator -> dataAccessCollaboratorService.delete(collaborator));
    return Response.ok().build();
  }
}
