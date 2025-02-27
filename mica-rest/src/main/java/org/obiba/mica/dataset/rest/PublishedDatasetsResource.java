/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/datasets")
@Scope("request")
public class PublishedDatasetsResource {

  @Inject
  private PublishedDatasetService publishedDatasetService;

  @Inject
  private Dtos dtos;

  @GET
  @Path("/_list")
  @Timed
  public Mica.DatasetsDto list(@QueryParam("from") @DefaultValue("0") int from,
                               @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
                               @QueryParam("order") String order, @QueryParam("study") String studyId, @QueryParam("query") String query) {

    PublishedDocumentService.Documents<Dataset> datasets = publishedDatasetService
        .find(from, limit, sort, order, studyId, query);

    Mica.DatasetsDto.Builder builder = Mica.DatasetsDto.newBuilder();

    builder.setFrom(datasets.getFrom()).setLimit(datasets.getLimit()).setTotal(datasets.getTotal());
    builder.addAllDatasets(datasets.getList().stream().map(dtos::asDto).collect(Collectors.toList()));

    return builder.build();
  }

  @GET
  @Path("_suggest")
  @Timed
  public List<String> suggest(@QueryParam("locale") @DefaultValue("en") String locale, @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("query") String query) {
    if (Strings.isNullOrEmpty(query)) return Lists.newArrayList();
    return publishedDatasetService.suggest(limit, locale, query);
  }
}
