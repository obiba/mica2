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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.core.domain.EntityStateFilter;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.network.service.DraftNetworkService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.search.AccessibleIdFilterBuilder;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;


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
                                           @QueryParam("sort") @DefaultValue("id") String sort,
                                           @QueryParam("order") @DefaultValue("asc") String order,
                                           @QueryParam("exclude") List<String> excludes,
                                           @QueryParam("filter") @DefaultValue("ALL") String filter,
                                           @Context HttpServletResponse response) { Stream<Network> result;
    long totalCount;

    EntityStateFilter entityStateFilter = EntityStateFilter.valueOf(filter);
    List<String> filteredIds = networkService.getIdsByStateFilter(entityStateFilter);

    Searcher.IdFilter accessibleIdFilter = AccessibleIdFilterBuilder.newBuilder()
      .aclService(subjectAclService)
      .resources(Lists.newArrayList("/draft/network"))
      .ids(filteredIds)
      .build();

    if(limit == null) limit = MAX_LIMIT;

    if(limit < 0) throw new IllegalArgumentException("limit cannot be negative");

    String ids = excludes.stream().map(s -> "id:" + s).collect(Collectors.joining(" "));

    if(!Strings.isNullOrEmpty(ids)) {
      if (Strings.isNullOrEmpty(query)) query = String.format("NOT(%s)", ids);
      else query += String.format(" AND NOT(%s)", ids);
    }

    DocumentService.Documents<Network> networkDocuments = draftNetworkService.find(from, limit, sort, order, studyId, query, null, null, accessibleIdFilter);
    totalCount = networkDocuments.getTotal();
    response.addHeader("X-Total-Count", Long.toString(totalCount));

    return networkDocuments.getList()
      .stream()
      .map(network -> dtos.asSummaryDto(network, true))
      .collect(toList());
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
