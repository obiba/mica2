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

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.core.domain.EntityStateFilter;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.DraftCollectedDatasetService;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.search.AccessibleIdFilterBuilder;
import org.obiba.mica.security.Roles;
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
                                    @QueryParam("sort") @DefaultValue("id") String sort,
                                    @QueryParam("order") @DefaultValue("asc") String order,
                                    @QueryParam("filter") @DefaultValue("ALL") String filter,
                                    @Context HttpServletResponse response) {
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

    DocumentService.Documents<StudyDataset> datasets = draftCollectedDatasetService.find(from, limit, sort, order,
      studyId, query, null, null, accessibleIdFilter);

    totalCount = datasets.getTotal();
    response.addHeader("X-Total-Count", Long.toString(totalCount));

    return datasets.getList()
      .stream()
      .map(dataset -> dtos.asDto(dataset, true))
      .collect(toList());
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

    if (SecurityUtils.getSubject().hasRole(Roles.MICA_EXTERNAL_EDITOR)) {
      subjectAclService.addPermission("/draft/collected-dataset", "VIEW,EDIT", dataset.getId());
      subjectAclService.addPermission("/draft/collected-dataset/" + dataset.getId(), "EDIT", "_status");
      subjectAclService.addPermission("/draft/collected-dataset/" + dataset.getId() + "/_attachments", "EDIT");

      subjectAclService.addPermission("/draft/file", "ADD,VIEW,EDIT", "/collected-dataset");
    }

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
