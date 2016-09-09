/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.rest;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;

@Path("/")
@RequiresAuthentication
public class PublishedNetworksResource {

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @GET
  @Path("/networks")
  @Timed
  public Mica.NetworksDto list(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
      @QueryParam("order") String order, @QueryParam("study") String studyId, @QueryParam("query") String query) {

    PublishedDocumentService.Documents<Network> networks = publishedNetworkService
        .find(from, limit, sort, order, studyId, query);

    Mica.NetworksDto.Builder builder = Mica.NetworksDto.newBuilder();

    builder.setFrom(networks.getFrom()).setLimit(networks.getLimit()).setTotal(networks.getTotal());
    builder.addAllNetworks(networks.getList().stream().map(dtos::asDto).collect(Collectors.toList()));

    return builder.build();
  }

  @Path("/network/{id}")
  public PublishedNetworkResource network(@PathParam("id") String id) {
    PublishedNetworkResource networkResource = applicationContext.getBean(PublishedNetworkResource.class);
    networkResource.setId(id);
    return networkResource;
  }

}
