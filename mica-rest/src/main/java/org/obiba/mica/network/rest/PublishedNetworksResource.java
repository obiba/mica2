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

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Path("/networks")
@Scope("request")
public class PublishedNetworksResource {

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @Inject
  private Dtos dtos;

  @GET
  @Path("/_list")
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

  @GET
  @Path("_suggest")
  @Timed
  public List<String> suggest(@QueryParam("locale") @DefaultValue("en") String locale, @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("query") String query) {
    if (Strings.isNullOrEmpty(query)) return Lists.newArrayList();
    return publishedNetworkService.suggest(limit, locale, query);
  }
}
