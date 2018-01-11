/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.network.NoSuchNetworkException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing Network.
 */
@Component
@Path("/network/{id}")
@Scope("request")
@RequiresAuthentication
public class PublishedNetworkResource {

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @GET
  @Timed
  public Mica.NetworkDto get(@PathParam("id") String id) {
    checkAccess(id);
    return dtos.asDto(getNetwork(id));
  }

  @Path("/file/{fileId}")
  public FileResource study(@PathParam("id") String id, @PathParam("fileId") String fileId) {
    checkAccess(id);
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    Network network = getNetwork(id);

    if(network.getLogo() == null) throw NoSuchEntityException.withId(Attachment.class, fileId);

    fileResource.setAttachment(network.getLogo());

    return fileResource;
  }

  private void checkAccess(String id) {
    subjectAclService.checkAccess("/network", id);
  }

  private Network getNetwork(String id) {
    Network network = publishedNetworkService.findById(id);
    if (network == null) throw NoSuchNetworkException.withId(id);
    return network;
  }
}
