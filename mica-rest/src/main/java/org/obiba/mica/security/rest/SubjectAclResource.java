package org.obiba.mica.security.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.obiba.mica.security.PermissionsUtils;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.obiba.mica.web.model.MicaSecurity.AclDto;

/**
 * REST controller for managing ACLs on a resource.
 */
@Component
@Scope("request")
public class SubjectAclResource {

  @Inject
  private SubjectAclService subjectAclService;

  private String resource;

  private String instance;

  private String fileResource;

  private String fileInstance;

  public void setResourceInstance(String resource, String instance) {
    this.resource = resource;
    this.instance = instance;
  }

  public void setFileResourceInstance(String resource, String instance) {
    fileResource = resource;
    fileInstance = instance;
  }

  @GET
  public List<AclDto> get() {
    subjectAclService.checkPermission(resource, "EDIT", instance);
    return subjectAclService.findByResourceInstance(resource, instance).stream().map(
      a -> AclDto.newBuilder().setType(a.getType().name()).setPrincipal(a.getPrincipal()).setResource(resource)
        .setRole(PermissionsUtils.asRole(a.getActions())).setInstance(instance).build()).collect(Collectors.toList());
  }

  @DELETE
  public Response delete(@QueryParam("principal") String principal, @QueryParam("type") String typeStr) {
    subjectAclService.checkPermission(resource, "EDIT", instance);
    SubjectAcl.Type type = SubjectAcl.Type.valueOf(typeStr.toUpperCase());
    subjectAclService.removeSubjectPermissions(type, principal, resource, instance);
    subjectAclService.removeSubjectPermissions(type, principal, fileResource, fileInstance);
    return Response.noContent().build();
  }

  @PUT
  public Response update(@QueryParam("principal") String principal, @QueryParam("type") String typeStr,
    @QueryParam("role") String role) {
    subjectAclService.checkPermission(resource, "EDIT", instance);
    SubjectAcl.Type type = SubjectAcl.Type.valueOf(typeStr.toUpperCase());
    String actions = PermissionsUtils.asActions(role.toUpperCase());
    subjectAclService.addSubjectPermission(type, principal, resource, actions, instance);
    String[] parts = fileInstance.split("/");
    subjectAclService.addSubjectPermission(type, principal, fileResource, actions, fileInstance);
    return Response.noContent().build();
  }

}
