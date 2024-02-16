/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.obiba.mica.file.FileUtils;
import org.obiba.mica.security.PermissionsUtils;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.stereotype.Component;

import static org.obiba.mica.file.FileUtils.normalizePath;
import static org.obiba.mica.web.model.MicaSecurity.AclDto;

/**
 * REST controller for managing ACLs on a published file from the draft file resource.
 */
@Component
@Path("/draft/file-access")
public class DraftFileAccessResource {

  @Inject
  private SubjectAclService subjectAclService;

  private final String resource = "/file";

  @GET
  @Path("/{path:.*}")
  public List<AclDto> get(@PathParam("path") String path) {
    String basePath = normalizePath(path);
    subjectAclService.checkPermission("/draft/file", "EDIT", basePath);

    return subjectAclService.findByResourceInstance(resource, basePath).stream().map(
      a -> AclDto.newBuilder().setType(a.getType().name()).setPrincipal(a.getPrincipal()).setResource(resource)
        .setRole(PermissionsUtils.asRole(a.getActions())).setInstance(FileUtils.decode(basePath)).build())
      .collect(Collectors.toList());
  }

  @DELETE
  @Path("/{path:.*}")
  public Response delete(@PathParam("path") String path, @QueryParam("principal") String principal,
    @QueryParam("type") String typeStr) {
    String basePath = normalizePath(path);
    subjectAclService.checkPermission("/draft/file", "DELETE", basePath);

    SubjectAcl.Type type = SubjectAcl.Type.valueOf(typeStr.toUpperCase());
    subjectAclService.removeSubjectPermissions(type, principal, resource, basePath);
    return Response.noContent().build();
  }

  @PUT
  @Path("/{path:.*}")
  public Response update(@PathParam("path") String path, @QueryParam("principal") String principal,
    @QueryParam("type") @DefaultValue("USER") String typeStr) {
    if(principal == null) return Response.status(Response.Status.BAD_REQUEST).build();
    String basePath = normalizePath(path);
    subjectAclService.checkPermission("/draft/file", "PUBLISH", basePath);

    SubjectAcl.Type type = SubjectAcl.Type.valueOf(typeStr.toUpperCase());
    String actions = PermissionsUtils.asActions("READER");
    subjectAclService.addSubjectPermission(type, principal, resource, actions, basePath);
    return Response.noContent().build();
  }

}
