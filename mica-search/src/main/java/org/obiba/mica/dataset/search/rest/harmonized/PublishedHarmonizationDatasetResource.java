/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.harmonized;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@Component
@Scope("request")
@Path("/harmonization-dataset/{id}")
@RequiresAuthentication
public class PublishedHarmonizationDatasetResource extends AbstractPublishedDatasetResource<HarmonizationDataset> {

  private static final Logger log = LoggerFactory.getLogger(PublishedHarmonizationDatasetResource.class);

  @PathParam("id")
  private String id;

  @Inject
  private HarmonizationDatasetService datasetService;

  /**
   * Get {@link org.obiba.mica.dataset.domain.HarmonizationDataset} from published index.
   *
   * @return
   */
  @GET
  public Mica.DatasetDto get() {
    return getDatasetDto(HarmonizationDataset.class, id);
  }

  /**
   * Get the {@link org.obiba.mica.dataset.domain.DatasetVariable}s from published index.
   *
   * @param queryString - Elasticsearch query string 'field1: value AND field2: value'
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @return
   */
  @GET
  @Path("/variables/_search")
  public Mica.DatasetVariablesDto queryVariables(@QueryParam("query") String queryString,
    @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
    @QueryParam("sort") String sort, @QueryParam("order") String order) {

    return getDatasetVariableDtos(queryString, id, DatasetVariable.Type.Dataschema , from, limit, sort, order);
  }

  /**
   * Get the {@link org.obiba.mica.dataset.domain.DatasetVariable}s from published index.
   *
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @return
   */
  @GET
  @Path("/variables")
  public Mica.DatasetVariablesDto getVariables(@QueryParam("from") @DefaultValue("0") int from,
    @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
    @QueryParam("order") String order) {

    return getDatasetVariableDtos(id, DatasetVariable.Type.Dataschema, from, limit, sort, order);
  }

  /**
   * Get the harmonized variable summaries for each of the queried dataschema variables and for each of the study.
   *
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @return
   */
  @GET
  @Path("/variables/harmonizations")
  public Mica.DatasetVariablesHarmonizationsDto getVariableHarmonizations(
    @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
    @QueryParam("sort") @DefaultValue("index") String sort, @QueryParam("order") @DefaultValue("asc") String order) {
    Mica.DatasetVariablesHarmonizationsDto.Builder builder = Mica.DatasetVariablesHarmonizationsDto.newBuilder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, id);
    Mica.DatasetVariablesDto variablesDto = getDatasetVariableDtos(id, DatasetVariable.Type.Dataschema, from, limit, sort, order);

    builder.setTotal(variablesDto.getTotal()).setLimit(variablesDto.getLimit()).setFrom(variablesDto.getFrom());

    variablesDto.getVariablesList()
      .forEach(variable -> builder.addVariableHarmonizations(getVariableHarmonizationDto(dataset, variable.getName())));

    return builder.build();
  }

  @GET
  @Path("/variables/harmonizations/_export")
  @Produces("text/csv")
  public Response getVariableHarmonizationsAsCsv(@QueryParam("sort") @DefaultValue("index") String sort,
    @QueryParam("order") @DefaultValue("asc") String order, @QueryParam("locale") @DefaultValue("en") String locale)
    throws IOException {

    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, id);
    Mica.DatasetVariablesHarmonizationsDto harmonizationVariables = getVariableHarmonizations(0, 999999, sort, order);

    CsvHarmonizationVariablesWriter writer = new CsvHarmonizationVariablesWriter(
      Lists.newArrayList("maelstrom", "Mlstr_harmo"));
    ByteArrayOutputStream values = writer.write(dataset, harmonizationVariables, locale);

    return Response.ok(values.toByteArray(), "text/csv")
      .header("Content-Disposition", "attachment; filename=\"" + id + ".csv\"").build();
  }

  @Path("/variable/{variable}")
  public PublishedDataschemaDatasetVariableResource getVariable(@PathParam("variable") String variable) {
    PublishedDataschemaDatasetVariableResource resource = applicationContext
      .getBean(PublishedDataschemaDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    return resource;
  }

  @GET
  @Path("/study/{study}/variables")
  public Mica.DatasetVariablesDto getVariables(@PathParam("study") String studyId,
    @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
    @QueryParam("sort") String sort, @QueryParam("order") String order) {
    return getDatasetVariableDtos(id, DatasetVariable.Type.Dataschema , from, limit, sort, order);
  }

  @Path("/study/{study}/variable/{variable}")
  public PublishedHarmonizedDatasetVariableResource getVariable(@PathParam("study") String studyId,
    @PathParam("variable") String variable) {
    PublishedHarmonizedDatasetVariableResource resource = applicationContext
      .getBean(PublishedHarmonizedDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    resource.setStudyId(studyId);
    return resource;
  }

  @POST
  @Path("/facets")
  public List<Search.QueryResultDto> getFacets(Search.QueryTermsDto query) {
    ImmutableList.Builder<Search.QueryResultDto> builder = ImmutableList.builder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, id);
    dataset.getStudyTables().forEach(t -> builder.add(datasetService.getFacets(query, t)));
    return builder.build();
  }

  @GET
  @Path("/contingency")
  public List<Search.QueryResultDto> getContingency(@QueryParam("variable") String variable,
    @QueryParam("by") String crossVariable) {
    if(Strings.isNullOrEmpty(variable) || Strings.isNullOrEmpty(crossVariable))
      throw new BadRequestException("Variable names are required for the contingency table");
    ImmutableList.Builder<Search.QueryResultDto> builder = ImmutableList.builder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, id);
    dataset.getStudyTables().forEach(t -> builder.add(datasetService.getContingencyTable(t, variable, crossVariable)));
    return builder.build();
  }

}
