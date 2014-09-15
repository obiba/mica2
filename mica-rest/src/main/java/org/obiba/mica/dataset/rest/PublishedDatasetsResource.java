/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;

@Path("/")
@RequiresAuthentication
public class PublishedDatasetsResource {

  @Inject
  private PublishedDatasetService publishedDatasetService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @GET
  @Path("/datasets")
  @Timed
  public Mica.DatasetsDto list(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort, @QueryParam("order") String order, @QueryParam("study") String studyId) {

    PublishedDocumentService.Documents<Dataset> datasets = publishedDatasetService.find(from, limit, sort, order, studyId);

    Mica.DatasetsDto.Builder builder = Mica.DatasetsDto.newBuilder();

    builder.setFrom(datasets.getFrom()).setLimit(datasets.getLimit()).setTotal(datasets.getTotal());
    builder.addAllDatasets(datasets.getList().stream().map(dtos::asDto).collect(Collectors.toList()));

    return builder.build();
  }

  @Path("/dataset/{id}")
  public PublishedDatasetResource dataset(@PathParam("id") String id) {
    PublishedDatasetResource resource = applicationContext.getBean(PublishedDatasetResource.class);
    resource.setId(id);
    return resource;
  }

}
