/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.obiba.mica.AbstractGitPersistableResource;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.network.NoSuchNetworkException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.security.rest.SubjectAclResource;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * REST controller for managing draft Study.
 */
@Component
@Scope("request")
public class DraftNetworkResource extends AbstractGitPersistableResource<NetworkState, Network> {

  @Inject
  private NetworkService networkService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  public Mica.NetworkDto get() {
    subjectAclService.checkPermission("/draft/network", "VIEW", id);
    return dtos.asDto(networkService.findById(id), true);
  }

  @PUT
  public Response update(@SuppressWarnings("TypeMayBeWeakened") Mica.NetworkDto networkDto) {
    subjectAclService.checkPermission("/draft/network", "EDIT", id);
    // ensure network exists
    networkService.findById(id);

    Network network = dtos.fromDto(networkDto);
    networkService.save(network);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_index")
  public Response index() {
    subjectAclService.checkPermission("/draft/network", "EDIT", id);
    networkService.index(id);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_publish")
  public Response publish(@QueryParam("cascading") @DefaultValue("UNDER_REVIEW") String cascadingScope) {
    subjectAclService.checkPermission("/draft/network", "PUBLISH", id);
    networkService.publish(id, true, PublishCascadingScope.valueOf(cascadingScope.toUpperCase()));
    return Response.noContent().build();
  }

  @DELETE
  @Path("/_publish")
  public Response unPublish() {
    subjectAclService.checkPermission("/draft/network", "PUBLISH", id);
    networkService.publish(id, false);
    return Response.noContent().build();
  }

  @DELETE
  public Response delete() {
    subjectAclService.checkPermission("/draft/network", "DELETE", id);
    try {
      networkService.delete(id);
    } catch (NoSuchNetworkException e) {
      // ignore
    }
    return Response.noContent().build();
  }

  @PUT
  @Path("/_status")
  public Response toUnderReview(@QueryParam("value") String status) {
    subjectAclService.checkPermission("/draft/network", "EDIT", id);
    networkService.updateStatus(id, RevisionStatus.valueOf(status.toUpperCase()));

    return Response.noContent().build();
  }

  @Path("/file/{fileId}")
  public FileResource network(@PathParam("fileId") String fileId) {
    subjectAclService.checkPermission("/draft/network", "VIEW", id);
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    Network network = networkService.findById(id);

    if(network.getLogo() == null) throw NoSuchEntityException.withId(Attachment.class, fileId);

    fileResource.setAttachment(network.getLogo());

    return fileResource;
  }

  @GET
  @Path("/commit/{commitId}/view")
  public Mica.NetworkDto getFromCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    subjectAclService.checkPermission("/draft/network", "VIEW", id);
    return dtos.asDto(networkService.getFromCommit(networkService.findDraft(id), commitId), true);
  }

  @Path("/permissions")
  public SubjectAclResource permissions() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/draft/network", id);
    subjectAclResource.setFileResourceInstance("/draft/file", "/network/" + id);
    return subjectAclResource;
  }

  @Path("/accesses")
  public SubjectAclResource accesses() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/network", id);
    subjectAclResource.setFileResourceInstance("/file", "/network/" + id);
    return subjectAclResource;
  }

  @Override
  protected String getId() {
    return id;
  }

  @Override
  protected AbstractGitPersistableService<NetworkState, Network> getService() {
    return networkService;
  }
}
