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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

  private static final Set<String> DOCUMENT_ROLES = Set.of("READER", "EDITOR", "REVIEWER");

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
   * document is never left without the transferred permission. {@code to} gets a permission on each of the document,
   * its files and its other resources where {@code from} had one: with the actions of {@code role} or, when omitted,
   * the same actions {@code from} had on that resource. When {@code to} already has a permission on that resource,
   * the actions are added to its own, so that the transfer never lowers its access (a permission granting all actions
   * is kept as it is). Its permissions on the resources {@code from} had none are left as they are. Comments keep
   * their author: {@code from} can edit or delete them only while it still has access to the document (see
   * CommentResource).
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
    String roleActions = role == null ? null : parseDocumentRole(role);

    // every check is done before the first write
    if (subjectAclService.findSubjectAcl(type, from, resource, instance).isEmpty())
      throw new BadRequestException("No permission to transfer from " + from);
    List<Grant> grants = Lists.newArrayList();
    for (String[] target : documentResources()) {
      SubjectAcl fromAcl = subjectAclService.findSubjectAcl(type, from, target[0], target[1]).orElse(null);
      if (fromAcl == null) continue;
      String actions = roleActions == null ? actionsOf(fromAcl) : roleActions;
      // not Optional.map: a null merge (to keeps its permission) would fall back to the transferred actions
      SubjectAcl toAcl = subjectAclService.findSubjectAcl(type, to, target[0], target[1]).orElse(null);
      grants.add(new Grant(target[0], target[1], toAcl == null ? actions : mergeActions(toAcl, actions)));
    }
    if (!userProfileService.userHas(to, Roles.MICA_EXTERNAL_EDITOR))
      throw new BadRequestException("Permissions can only be transferred to an external editor: " + to);

    grants.stream().filter(grant -> grant.actions() != null)
      .forEach(grant -> subjectAclService.addSubjectPermission(type, to, grant.resource(), grant.actions(), grant.instance()));
    grants.forEach(grant -> subjectAclService.removeSubjectPermissions(type, from, grant.resource(), grant.instance()));

    return Response.noContent().build();
  }

  /**
   * @param actions null when {@code to} keeps its permission as it is
   */
  private record Grant(String resource, String instance, String actions) {}

  /**
   * The actions of the permission {@code to} already has, with the transferred ones added, or null when that
   * permission grants all actions already.
   */
  private static String mergeActions(SubjectAcl toAcl, String actions) {
    Set<String> merged = toAcl.getActions().stream().filter(a -> !Strings.isNullOrEmpty(a))
      .collect(Collectors.toCollection(LinkedHashSet::new));
    if (merged.isEmpty()) return null;
    for (String action : actions.split(",")) merged.add(action);
    return String.join(",", merged);
  }

  /**
   * The resource and instance pairs of the document: the document first, then its files and its other resources.
   */
  private List<String[]> documentResources() {
    List<String[]> targets = Lists.newArrayList();
    targets.add(new String[] { resource, instance });
    if (fileResource != null && fileInstance != null) targets.add(new String[] { fileResource, fileInstance });
    otherResources.forEach(other -> targets.add(new String[] { mergeWithResourceName(other), instance }));
    return targets;
  }

  /**
   * The actions of the permission, as they are. A permission without actions grants all of them: it cannot be
   * given through the document permissions, so it is not handed over to an external editor implicitly.
   */
  private static String actionsOf(SubjectAcl acl) {
    String actions = acl.getActions().stream().filter(a -> !Strings.isNullOrEmpty(a)).collect(Collectors.joining(","));
    if (actions.isEmpty())
      throw new BadRequestException("The permission of " + acl.getPrincipal() + " on " + acl.getResource()
        + " grants all actions, give the role to transfer");
    return actions;
  }

  private static SubjectAcl.Type parseType(String typeStr) {
    try {
      return SubjectAcl.Type.valueOf(typeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid principal type: " + typeStr);
    }
  }

  /**
   * The actions of a document role: ADMINISTRATOR has no actions, which grants all of them, and ANALYST is not a
   * document role.
   */
  private static String parseDocumentRole(String role) {
    String name = role.toUpperCase();
    if (!DOCUMENT_ROLES.contains(name)) throw new BadRequestException("Invalid role: " + role);
    return PermissionsUtils.asActions(name);
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
