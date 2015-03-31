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

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.core.security.Roles;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.network.NoSuchNetworkException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing draft Study.
 */
@Component
@Scope("request")
@RequiresAuthentication
public class DraftNetworkResource {

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
  @Timed
  public Mica.NetworkDto get() {
    return dtos.asDto(networkService.findById(id));
  }

  @PUT
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(@SuppressWarnings("TypeMayBeWeakened") Mica.NetworkDto networkDto) {
    // ensure network exists
    networkService.findById(id);

    Network network = dtos.fromDto(networkDto);
    networkService.save(network);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_index")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response index() {
    networkService.index(id);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_publish")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response publish() {
    networkService.publish(id, true);
    return Response.noContent().build();
  }

  @DELETE
  @Path("/_publish")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response unPublish() {
    networkService.publish(id, false);
    return Response.noContent().build();
  }

  @DELETE
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response delete() {
    try {
      networkService.delete(id);
    } catch (NoSuchNetworkException e) {
      // ignore
    }
    return Response.noContent().build();
  }

  @Path("/file/{fileId}")
  public FileResource study(@PathParam("fileId") String fileId) {
    FileResource studyResource = applicationContext.getBean(FileResource.class);
    studyResource.setPersistable(networkService.findById(id));
    studyResource.setFileId(fileId);

    return studyResource;
  }

}
