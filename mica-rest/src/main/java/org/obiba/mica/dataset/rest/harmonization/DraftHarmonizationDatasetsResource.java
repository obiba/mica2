/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
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
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.service.DraftHarmonizationDatasetService;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.security.service.SubjectAclService;
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
public class DraftHarmonizationDatasetsResource {
  private static final int MAX_LIMIT = 10000; //default ElasticSearch limit

  @Inject
  private HarmonizationDatasetService datasetService;

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
  @Path("/harmonization-datasets")
  @Timed
  public List<Mica.DatasetDto> list(@QueryParam("study") String studyId, @QueryParam("query") String query,
                                    @QueryParam("from") @DefaultValue("0") Integer from,
                                    @QueryParam("limit") Integer limit, @Context HttpServletResponse response) {
    Stream<Mica.DatasetDto> result;
    long totalCount;

    if(limit == null) limit = MAX_LIMIT;

    if(limit < 0) throw new IllegalArgumentException("limit cannot be negative");

    if(Strings.isNullOrEmpty(query)) {
      List<HarmonizationDataset> datasets = datasetService.findAllDatasets(studyId).stream()
        .filter(s -> subjectAclService.isPermitted("/draft/harmonization-dataset", "VIEW", s.getId())).collect(toList());
      totalCount = datasets.size();
      result = datasets.stream().map(d -> dtos.asDto(d, true)).skip(from).limit(limit);
    } else {
      DocumentService.Documents<HarmonizationDataset> datasets = draftDatasetService.find(from, limit, null, null, studyId, query);
      totalCount = datasets.getTotal();
      result = datasetService.findAllDatasets(datasets.getList().stream().map(Dataset::getId).collect(toList())).stream().map(d -> dtos.asDto(d, true));
    }

    response.addHeader("X-Total-Count", Long.toString(totalCount));

    return result.collect(toList());

  }

  @POST
  @Path("/harmonization-datasets")
  @Timed
  @RequiresPermissions({ "/draft/harmonization-dataset:ADD" })
  public Response create(Mica.DatasetDto datasetDto, @Context UriInfo uriInfo) {
    Dataset dataset = dtos.fromDto(datasetDto);
    if(!(dataset instanceof HarmonizationDataset))
      throw new IllegalArgumentException("An harmonization dataset is expected");

    datasetService.save((HarmonizationDataset) dataset);
    return Response
      .created(uriInfo.getBaseUriBuilder().segment("draft", "harmonization-dataset", dataset.getId()).build()).build();
  }

  @PUT
  @Path("/harmonization-datasets/_index")
  @Timed
  @RequiresPermissions({ "/draft/harmonization-dataset:PUBLISH" })
  public Response reIndex() {
    helper.indexAll();
    return Response.noContent().build();
  }

  @Path("/harmonization-dataset/{id}")
  public DraftHarmonizationDatasetResource dataset(@PathParam("id") String id) {
    DraftHarmonizationDatasetResource resource = applicationContext.getBean(DraftHarmonizationDatasetResource.class);
    resource.setId(id);
    return resource;
  }

  @Component
  public static class Helper {

    @Inject
    private HarmonizationDatasetService harmonizationDatasetService;

    @Async
    public void indexAll() {
      harmonizationDatasetService.indexAll();
    }
  }
}
