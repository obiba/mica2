/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.security.rest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.security.PermissionsUtils;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.model.MicaSecurity.AclDto;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * REST controller for managing ACLs on a resource.
 */
@Component
@Scope("request")
public class SubjectAclResource {

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private UserProfileService userProfileService;

  private String resource;

  private String instance;

  private String fileResource;

  private String fileInstance;

  private Set<String> otherResources = new HashSet<String>();

  public void setResourceInstance(String resource, String instance) {
    this.resource = resource;
    this.instance = instance;
  }

  public void setFileResourceInstance(String resource, String instance) {
    fileResource = resource;
    fileInstance = instance;
  }

  public void addOtherResourceName(String resource) {
    otherResources.add(resource);
  }

  @GET
  public List<AclDto> get() {
    checkPermission();

    Map<String, AclDto.Builder> builderMap = subjectAclService.findByResourceInstance(resource, instance).stream().map(
      a -> AclDto.newBuilder().setType(a.getType().name()).setPrincipal(a.getPrincipal()).setResource(resource)
        .setRole(PermissionsUtils.asRole(a.getActions())).setInstance(FileUtils.decode(instance)))
      .collect(Collectors.toMap(AclDto.Builder::getPrincipal, aclDto -> aclDto));

    for (String otherResource : otherResources) {
      subjectAclService.findByResourceInstance(mergeWithResourceName(otherResource), instance).forEach(subjectAcl -> builderMap.get(subjectAcl.getPrincipal()).addOtherResources(otherResource));
    }

    if (fileResource != null && fileInstance != null) {
      subjectAclService.findByResourceInstance(fileResource, fileInstance).forEach(subjectAcl -> {
        AclDto.Builder builder = builderMap.get(subjectAcl.getPrincipal());
        if (builder != null) builder.setFile(true);
      });
    }

    return builderMap.values().stream().map(AclDto.Builder::build).collect(Collectors.toList());
  }

  @DELETE
  public Response delete(@QueryParam("principal") String principal, @QueryParam("type") String typeStr) {
    checkPermission();

    SubjectAcl.Type type = SubjectAcl.Type.valueOf(typeStr.toUpperCase());
    subjectAclService.removeSubjectPermissions(type, principal, resource, instance);
    subjectAclService.removeSubjectPermissions(type, principal, fileResource, fileInstance);

    otherResources.forEach(otherResourceName -> subjectAclService.removeSubjectPermissions(type, principal, mergeWithResourceName(otherResourceName), instance));

    return Response.noContent().build();
  }

  @PUT
  public Response update(@QueryParam("principal") String principal,
    @QueryParam("type") @DefaultValue("USER") String typeStr, @QueryParam("role") @DefaultValue("READER") String role,
    @QueryParam("file") @DefaultValue("true") boolean file, @QueryParam("config") @DefaultValue("false") boolean config, @QueryParam("otherResources") List<String> others) {
    if(principal == null) return Response.status(Response.Status.BAD_REQUEST).build();
    checkPermission();

    SubjectAcl.Type type = SubjectAcl.Type.valueOf(typeStr.toUpperCase());
    String actions = PermissionsUtils.asActions(config ? role.toUpperCase() : (isDraft() ? role.toUpperCase() : "READER"));
    subjectAclService.addSubjectPermission(type, principal, resource, actions, instance);

    if (fileResource != null && fileInstance != null) {
      subjectAclService.removeSubjectPermissions(type, principal, fileResource, fileInstance);
      if (file) {
        subjectAclService.addSubjectPermission(type, principal, fileResource, actions, fileInstance);
      }
    }

    otherResources.forEach(otherResourceName -> subjectAclService.removeSubjectPermissions(type, principal, mergeWithResourceName(otherResourceName), instance));
    if (others != null && others.size() > 0) {
      others.stream().filter(otherResources::contains).forEach(otherResourceName -> subjectAclService.addSubjectPermission(type, principal, mergeWithResourceName(otherResourceName), actions, instance));
    }

    return Response.noContent().build();
  }

  /**
   * Hand the permissions of one user over to another: grant {@code to} first, then revoke {@code from}, so the
   * document is never left without the transferred permission. {@code to} gets the file and other resource
   * permissions that {@code from} had, with {@code role} or, when omitted, the role {@code from} had. A permission
   * {@code to} already had is replaced. Comments keep their author: {@code from} can edit or delete them only while it
   * still has access to the document (see CommentResource).
   * Administrators only: handing a document over to another owner is not an editor decision. Users only, and
   * {@code to} must be an external editor: the only users whose access to a document comes from its ACLs.
   */
  @PUT
  @Path("/_transfer")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response transfer(@QueryParam("from") String from, @QueryParam("to") String to,
    @QueryParam("type") @DefaultValue("USER") String typeStr, @QueryParam("role") String role) {
    if (!isDraftDocument()) throw new BadRequestException("Permissions can only be transferred on a draft document");
    if (Strings.isNullOrEmpty(from) || Strings.isNullOrEmpty(to)) throw new BadRequestException("Both from and to principals are required");
    if (from.equals(to)) throw new BadRequestException("Cannot transfer permissions to the same principal");
    checkPermission();

    SubjectAcl.Type type = parseType(typeStr);
    if (type != SubjectAcl.Type.USER) throw new BadRequestException("Permissions can only be transferred between users");
    SubjectAcl fromAcl = findSubjectAcl(type, from, resource, instance)
      .orElseThrow(() -> new BadRequestException("No permission to transfer from " + from));
    String actions = parseActions(role == null ? PermissionsUtils.asRole(fromAcl.getActions()) : role);
    if (!userProfileService.userHas(to, Roles.MICA_EXTERNAL_EDITOR))
      throw new BadRequestException("Permissions can only be transferred to an external editor: " + to);

    // grant
    subjectAclService.addSubjectPermission(type, to, resource, actions, instance);
    boolean file = fileResource != null && fileInstance != null && findSubjectAcl(type, from, fileResource, fileInstance).isPresent();
    if (file) subjectAclService.addSubjectPermission(type, to, fileResource, actions, fileInstance);
    List<String> others = otherResources.stream()
      .filter(other -> findSubjectAcl(type, from, mergeWithResourceName(other), instance).isPresent())
      .toList();
    others.forEach(other -> subjectAclService.addSubjectPermission(type, to, mergeWithResourceName(other), actions, instance));

    // revoke
    subjectAclService.removeSubjectPermissions(type, from, resource, instance);
    if (file) subjectAclService.removeSubjectPermissions(type, from, fileResource, fileInstance);
    others.forEach(other -> subjectAclService.removeSubjectPermissions(type, from, mergeWithResourceName(other), instance));

    return Response.noContent().build();
  }

  private Optional<SubjectAcl> findSubjectAcl(SubjectAcl.Type type, String principal, String res, String inst) {
    return subjectAclService.findByResourceInstance(res, inst).stream()
      .filter(acl -> acl.getType() == type && principal.equals(acl.getPrincipal()))
      .findFirst();
  }

  private static SubjectAcl.Type parseType(String typeStr) {
    try {
      return SubjectAcl.Type.valueOf(typeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid principal type: " + typeStr);
    }
  }

  private static String parseActions(String role) {
    try {
      return PermissionsUtils.asActions(role.toUpperCase());
    } catch (NoSuchElementException e) {
      throw new BadRequestException("Invalid role: " + role);
    }
  }

  private String mergeWithResourceName(String otherResourceName) {
    return resource + "/" + otherResourceName;
  }

  private void checkPermission() {
    String res = resource.startsWith("/draft") ? resource : "/draft" + resource;
    subjectAclService.checkPermission(res, "EDIT", instance);
  }

  private boolean isDraft() {
    return resource.startsWith("/draft");
  }

  /**
   * A draft document, not the permissions on all the documents of a type (instance "*").
   */
  private boolean isDraftDocument() {
    return isDraft() && !Strings.isNullOrEmpty(instance) && !"*".equals(instance);
  }
}
