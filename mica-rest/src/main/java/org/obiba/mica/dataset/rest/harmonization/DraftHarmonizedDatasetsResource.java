/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.harmonization;

import java.util.List;
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
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.core.domain.EntityStateFilter;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.service.DraftHarmonizationDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.search.AccessibleIdFilterBuilder;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

@Component
@Scope("request")
@Path("/draft")
public class DraftHarmonizedDatasetsResource {
  private static final int MAX_LIMIT = 10000; //default ElasticSearch limit

  @Inject
  private HarmonizedDatasetService datasetService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private Helper helper;

  @Inject
  private DraftHarmonizationDatasetService draftDatasetService;

  /**
   * Get all {@link HarmonizationDataset}, optionally filtered by study.
   *
   * @param studyId can be null, in which case all datasets are returned
   * @return
   */
  @GET
  @Path("/harmonized-datasets")
  @Timed
  public List<Mica.DatasetDto> list(@QueryParam("study") String studyId,
                                    @QueryParam("query") String query,
                                    @QueryParam("from") @DefaultValue("0") Integer from,
                                    @QueryParam("limit") Integer limit,
                                    @QueryParam("filter") @DefaultValue("ALL") String filter,
                                    @Context HttpServletResponse response) {
    Stream<Mica.DatasetDto> result;
    long totalCount;

    EntityStateFilter entityStateFilter = EntityStateFilter.valueOf(filter);
    List<String> filteredIds = datasetService.getIdsByStateFilter(entityStateFilter);

    Searcher.IdFilter accessibleIdFilter = AccessibleIdFilterBuilder.newBuilder()
      .aclService(subjectAclService)
      .resources(Lists.newArrayList("/draft/harmonized-dataset"))
      .ids(filteredIds)
      .build();

    if(limit == null) limit = MAX_LIMIT;

    if(limit < 0) throw new IllegalArgumentException("limit cannot be negative");

    DocumentService.Documents<HarmonizationDataset> datasets = draftDatasetService.find(from, limit, null, null, studyId, query, null, null, accessibleIdFilter);
    totalCount = datasets.getTotal();
    result = datasetService.findAllDatasets(datasets.getList().stream().map(Dataset::getId).collect(toList())).stream().map(d -> dtos.asDto(d, true));

    response.addHeader("X-Total-Count", Long.toString(totalCount));

    return result.collect(toList());

  }

  @POST
  @Path("/harmonized-datasets")
  @Timed
  @RequiresPermissions({ "/draft/harmonized-dataset:ADD" })
  public Response create(Mica.DatasetDto datasetDto, @Context UriInfo uriInfo,
                         @Nullable @QueryParam("comment") String comment) {
    Dataset dataset = dtos.fromDto(datasetDto);
    if(!(dataset instanceof HarmonizationDataset))
      throw new IllegalArgumentException("An harmonization dataset is expected");

    datasetService.save((HarmonizationDataset) dataset, comment);
    return Response
      .created(uriInfo.getBaseUriBuilder().segment("draft", "harmonized-dataset", dataset.getId()).build()).build();
  }

  @PUT
  @Path("/harmonized-datasets/_index")
  @Timed
  @RequiresPermissions({ "/draft/harmonized-dataset:PUBLISH" })
  public Response reIndex(@Nullable @QueryParam("id") List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      helper.indexAll();
    } else {
      helper.indexByIds(ids);
    }
    return Response.noContent().build();
  }

  @Path("/harmonized-dataset/{id}")
  public DraftHarmonizedDatasetResource dataset(@PathParam("id") String id) {
    DraftHarmonizedDatasetResource resource = applicationContext.getBean(DraftHarmonizedDatasetResource.class);
    resource.setId(id);
    return resource;
  }

  @Component
  public static class Helper {

    @Inject
    private HarmonizedDatasetService harmonizedDatasetService;

    @Async
    public void indexAll() {
      harmonizedDatasetService.indexAll();
    }

    @Async
    public void indexByIds(List<String> ids) {
      harmonizedDatasetService.indexByIds(ids, true);
    }
  }
}
