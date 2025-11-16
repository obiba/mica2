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

import jakarta.annotation.Nullable;
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
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.service.DraftHarmonizationDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
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
                                    @QueryParam("sort") @DefaultValue("id") String sort,
                                    @QueryParam("order") @DefaultValue("asc") String order,
                                    @QueryParam("filter") @DefaultValue("ALL") String filter,
                                    @Context HttpServletResponse response) {
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

    DocumentService.Documents<HarmonizationDataset> datasets = draftDatasetService.find(from, limit, sort,
      order, studyId, query, null, null, accessibleIdFilter);
    totalCount = datasets.getTotal();
    response.addHeader("X-Total-Count", Long.toString(totalCount));

    return datasets.getList()
      .stream()
      .map(dataset -> dtos.asDto(dataset, true))
      .collect(toList());
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

    if (SecurityUtils.getSubject().hasRole(Roles.MICA_EXTERNAL_EDITOR)) {
      subjectAclService.addPermission("/draft/harmonized-dataset", "VIEW,EDIT", dataset.getId());
      subjectAclService.addPermission("/draft/harmonized-dataset/" + dataset.getId(), "EDIT", "_status");
      subjectAclService.addPermission("/draft/harmonized-dataset/" + dataset.getId() + "/_attachments", "EDIT");
    }

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
