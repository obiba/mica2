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

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
@Path("/draft")
public class DraftNetworksResource {

  @Inject
  private NetworkService networkService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @GET
  @Path("/networks")
  @Timed
  public List<Mica.NetworkDto> list(@QueryParam("study") String studyId) {
    return networkService.findAllNetworks(studyId).stream()
      .filter(n -> subjectAclService.isPermitted("/draft/network", "VIEW", n.getId()))
      .sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).map(n -> dtos.asDto(n, true)).collect(Collectors.toList());
  }

  @POST
  @Path("/networks")
  @Timed
  @RequiresPermissions("/draft/network:ADD")
  public Response create(Mica.NetworkDto networkDto, @Context UriInfo uriInfo) {
    Network network = dtos.fromDto(networkDto);

    networkService.save(network);
    return Response.created(uriInfo.getBaseUriBuilder().segment("draft", "network", network.getId()).build()).build();
  }

  @PUT
  @Path("/networks/_index")
  @Timed
  @RequiresPermissions("/draft/network:PUBLISH")
  public Response reIndex() {
    networkService.indexAll();
    return Response.noContent().build();
  }

  @Path("/network/{id}")
  public DraftNetworkResource dataset(@PathParam("id") String id) {
    DraftNetworkResource resource = applicationContext.getBean(DraftNetworkResource.class);
    resource.setId(id);

    return resource;
  }
}
