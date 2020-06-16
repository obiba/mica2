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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.network.service.DraftNetworkService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;


@Component
@Scope("request")
@Path("/draft")
public class DraftNetworksResource {

  private static final int MAX_LIMIT = 10000; //default ElasticSearch limit

  @Inject
  private NetworkService networkService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private EventBus eventBus;

  @Inject
  private DraftNetworkService draftNetworkService;

  @GET
  @Path("/networks")
  @Timed
  public List<Mica.NetworkSummaryDto> list(@QueryParam("study") String studyId,
                                           @QueryParam("query") String query,
                                           @QueryParam("from") @DefaultValue("0") Integer from,
                                           @QueryParam("limit") Integer limit,
                                           @QueryParam("exclude") List<String> excludes,
                                           @Context HttpServletResponse response) { Stream<Network> result;
    long totalCount;

    if(limit == null) limit = MAX_LIMIT;

    if(limit < 0) throw new IllegalArgumentException("limit cannot be negative");

    String ids = excludes.stream().map(s -> "id:" + s).collect(Collectors.joining(" "));

    if(!Strings.isNullOrEmpty(ids)) {
      if (Strings.isNullOrEmpty(query)) query = String.format("NOT(%s)", ids);
      else query += String.format(" AND NOT(%s)", ids);
    }

    if(Strings.isNullOrEmpty(query)) {
      List<Network> networks = networkService.findAllNetworks(studyId).stream()
        .filter(n -> subjectAclService.isPermitted("/draft/network", "VIEW", n.getId())).collect(toList());
      totalCount = networks.size();
      result = networks.stream().sorted((o1, o2) -> o1.getId().compareTo(o2.getId()))//
        .skip(from).limit(limit);
    } else {
      DocumentService.Documents<Network> networkDocuments = draftNetworkService.find(from, limit, null, null, studyId, query);
      totalCount = networkDocuments.getTotal();
      result = networkService.findAllNetworks(networkDocuments.getList().stream().map(AbstractGitPersistable::getId).collect(toList())).stream();
    }

    response.addHeader("X-Total-Count", Long.toString(totalCount));

    return result.map(n -> dtos.asSummaryDto(n, true)).collect(toList());
  }

  @POST
  @Path("/networks")
  @Timed
  @RequiresPermissions("/draft/network:ADD")
  public Response create(Mica.NetworkDto networkDto, @Context UriInfo uriInfo,
                         @Nullable @QueryParam("comment") String comment) {
    Network network = dtos.fromDto(networkDto);

    networkService.save(network, comment);
    return Response.created(uriInfo.getBaseUriBuilder().segment("draft", "network", network.getId()).build()).build();
  }

  @PUT
  @Path("/networks/_index")
  @Timed
  @RequiresPermissions("/draft/network:PUBLISH")
  public Response reIndex(@Nullable @QueryParam("id") List<String> ids) {
    eventBus.post(new IndexNetworksEvent(ids));
    return Response.noContent().build();
  }

  @Path("/network/{id}")
  public DraftNetworkResource network(@PathParam("id") String id) {
    DraftNetworkResource resource = applicationContext.getBean(DraftNetworkResource.class);
    resource.setId(id);

    return resource;
  }
}
