/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.collection;

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
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.DraftCollectedDatasetService;
import org.obiba.mica.dataset.service.CollectedDatasetService;
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
public class DraftCollectedDatasetsResource {

  private static final int MAX_LIMIT = 10000; //default ElasticSearch limit

  private final CollectedDatasetService datasetService;

  private final SubjectAclService subjectAclService;

  private final Dtos dtos;

  private final ApplicationContext applicationContext;

  private final Helper helper;

  private final DraftCollectedDatasetService draftCollectedDatasetService;

  @Inject
  public DraftCollectedDatasetsResource(CollectedDatasetService datasetService,
                                        SubjectAclService subjectAclService,
                                        Dtos dtos,
                                        ApplicationContext applicationContext,
                                        Helper helper,
                                        DraftCollectedDatasetService draftCollectedDatasetService) {
    this.datasetService = datasetService;
    this.subjectAclService = subjectAclService;
    this.dtos = dtos;
    this.applicationContext = applicationContext;
    this.helper = helper;
    this.draftCollectedDatasetService = draftCollectedDatasetService;
  }

  /**
   * Get all {@link org.obiba.mica.dataset.domain.StudyDataset}, optionally filtered by study.
   *
   * @param studyId can be null, in which case all datasets are returned
   * @return
   */
  @GET
  @Path("/collected-datasets")
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
      .resources(Lists.newArrayList("/draft/collected-dataset"))
      .ids(filteredIds)
      .build();

    if(limit == null) limit = MAX_LIMIT;

    if(limit < 0) throw new IllegalArgumentException("limit cannot be negative");

    DocumentService.Documents<StudyDataset> datasets = draftCollectedDatasetService.find(from, limit, null, null, studyId, query, null, null, accessibleIdFilter);
    totalCount = datasets.getTotal();
    result = datasetService.findAllDatasets(datasets.getList().stream().map(Dataset::getId).collect(toList())).stream().map(d -> dtos.asDto(d, true));

    response.addHeader("X-Total-Count", Long.toString(totalCount));

    return result.collect(toList());
  }

  @POST
  @Path("/collected-datasets")
  @Timed
  @RequiresPermissions({ "/draft/collected-dataset:ADD" })
  public Response create(Mica.DatasetDto datasetDto, @Context UriInfo uriInfo,
                         @Nullable @QueryParam("comment") String comment) {
    Dataset dataset = dtos.fromDto(datasetDto);
    if(!(dataset instanceof StudyDataset)) throw new IllegalArgumentException("A collected dataset is expected");

    datasetService.save((StudyDataset) dataset, comment);
    return Response.created(uriInfo.getBaseUriBuilder().segment("draft", "collected-dataset", dataset.getId()).build())
      .build();
  }

  @PUT
  @Path("/collected-datasets/_index")
  @Timed
  @RequiresPermissions({ "/draft/collected-dataset:PUBLISH" })
  public Response reIndex(@Nullable @QueryParam("id") List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      helper.indexAll();
    } else {
      helper.indexByIds(ids);
    }
    return Response.noContent().build();
  }

  @Path("/collected-dataset/{id}")
  public DraftCollectedDatasetResource dataset(@PathParam("id") String id) {
    DraftCollectedDatasetResource resource = applicationContext.getBean(DraftCollectedDatasetResource.class);
    resource.setId(id);
    return resource;
  }

  @Component
  public static class Helper {

    @Inject
    private CollectedDatasetService collectedDatasetService;

    @Async
    public void indexAll() {
      collectedDatasetService.indexAll();
    }

    @Async
    public void indexByIds(List<String> ids) {
      collectedDatasetService.indexByIds(ids, true);
    }
  }

}
