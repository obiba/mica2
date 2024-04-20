/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.harmonization;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Scope("request")
@Path("/harmonized-dataset/{id}")
public class PublishedHarmonizedDatasetResource extends AbstractPublishedDatasetResource<HarmonizationDataset> {

  private static final Logger log = LoggerFactory.getLogger(PublishedHarmonizedDatasetResource.class);

  /**
   * Get {@link HarmonizationDataset} from published index.
   *
   * @return
   */
  @GET
  @Timed
  public Mica.DatasetDto get(@PathParam("id") String id) {
    checkDatasetAccess(id);
    return getDatasetDto(HarmonizationDataset.class, id);
  }

  /**
   * Get the {@link DatasetVariable}s from published index.
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
  @Timed
  public Mica.DatasetVariablesDto queryVariables(@PathParam("id") String id, @QueryParam("query") String queryString,
                                                 @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
                                                 @QueryParam("sort") String sort, @QueryParam("order") String order) {
    checkDatasetAccess(id);
    return getDatasetVariableDtos(queryString, id, DatasetVariable.Type.Dataschema, from, limit, sort, order);
  }

  /**
   * Get the {@link DatasetVariable}s from published index.
   *
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @return
   */
  @GET
  @Path("/variables")
  @Timed
  public Mica.DatasetVariablesDto getVariables(@PathParam("id") String id,
                                               @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
                                               @QueryParam("sort") String sort, @QueryParam("order") String order) {
    checkDatasetAccess(id);
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
  @Timed
  @Deprecated
  public Mica.DatasetVariablesHarmonizationsDto getVariableHarmonizations(@PathParam("id") String id,
                                                                          @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
                                                                          @QueryParam("sort") @DefaultValue("index") String sort, @QueryParam("order") @DefaultValue("asc") String order) {
    return getVariableHarmonizationsInternal(id, from, limit, sort, order, true);
  }

  @GET
  @Path("/variables/harmonizations/_summary")
  @Timed
  public Mica.DatasetVariablesHarmonizationSummaryDto getVariableHarmonizationsSummary(@PathParam("id") String id,
                                                                                       @QueryParam("query") String query,
                                                                                       @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
                                                                                       @QueryParam("sort") @DefaultValue("index") String sort, @QueryParam("order") @DefaultValue("asc") String order) {
    return getVariableHarmonizationsSummaryInternal(id, query, from, limit, sort, order, true);
  }

  @GET
  @Path("/variables/harmonizations/_export")
  @Produces("text/csv")
  @Timed
  public Response getVariableHarmonizationsAsCsv(@PathParam("id") String id,
                                                 @QueryParam("sort") @DefaultValue("index") String sort, @QueryParam("order") @DefaultValue("asc") String order,
                                                 @QueryParam("locale") @DefaultValue("en") String locale) throws IOException {
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, id);
    Mica.DatasetVariablesHarmonizationsDto harmonizationVariables =
      getVariableHarmonizationsInternal(id, 0, 999999, sort, order, false);

    CsvHarmonizationVariablesWriter writer = new CsvHarmonizationVariablesWriter(
      Lists.newArrayList("maelstrom", "Mlstr_harmo"));
    ByteArrayOutputStream values = writer.write(dataset, harmonizationVariables, locale);

    return Response.ok(values.toByteArray(), "text/csv")
      .header("Content-Disposition", "attachment; filename=\"" + id + ".csv\"").build();
  }

  @Path("/variable/{variable}")
  public PublishedDataschemaDatasetVariableResource getVariable(@PathParam("id") String id,
                                                                @PathParam("variable") String variable) {
    checkDatasetAccess(id);
    PublishedDataschemaDatasetVariableResource resource = applicationContext
      .getBean(PublishedDataschemaDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    return resource;
  }

  @GET
  @Path("/study/{study}/variables")
  @Timed
  public Mica.DatasetVariablesDto getVariables(@PathParam("id") String id, @PathParam("study") String studyId,
                                               @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
                                               @QueryParam("sort") String sort, @QueryParam("order") String order) {
    checkDatasetAccess(id);
    String rql = String.format("and(eq(datasetId,%s),eq(studyId,%s),eq(variableType,%s))", id, studyId, DatasetVariable.Type.Dataschema.toString());
    return getDatasetVariableDtosInternal(rql, from, limit, sort, order);
  }

  @Path("/study/{study}/population/{population}/data-collection-event/{dce}/variable/{variable}")
  public PublishedHarmonizedDatasetVariableResource getVariable(@PathParam("id") String id,
                                                                @PathParam("study") String studyId, @PathParam("population") String populationId, @PathParam("dce") String dceId,
                                                                @PathParam("variable") String variable) {
    checkDatasetAccess(id);
    PublishedHarmonizedDatasetVariableResource resource = applicationContext
      .getBean(PublishedHarmonizedDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    resource.setStudyId(studyId);
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, id);
    dataset.getStudyTables().stream().filter(t -> t.appliesTo(studyId, populationId, dceId)).forEach(t -> {
      resource.setTableType(t instanceof StudyTable
        ? DatasetVariable.OPAL_STUDY_TABLE_PREFIX
        : DatasetVariable.OPAL_HARMONIZATION_TABLE_PREFIX);
    });
    return resource;
  }

  @GET
  @Path("/tables/harmonizations")
  @Override
  public Map<Object, Object> getHarmonizationStatusAggregation(@PathParam("id") String id,
                                                @QueryParam("size") @DefaultValue("9999") int size,
                                                @QueryParam("agg") @DefaultValue("tableUid") String agg,
                                                @QueryParam("field") @DefaultValue("attributes.Mlstr_harmo__status.und") String fieldName) {
    return super.getHarmonizationStatusAggregation(id, size, agg, fieldName);
  }

  private Mica.DatasetVariablesHarmonizationsDto getVariableHarmonizationsInternal(String id,
                                                                                   int from, int limit, String sort, String order, boolean includeSummaries) {

    checkDatasetAccess(id);
    Mica.DatasetVariablesHarmonizationsDto.Builder builder = Mica.DatasetVariablesHarmonizationsDto.newBuilder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, id);
    Mica.DatasetVariablesDto variablesDto = getDatasetVariableDtos(id, DatasetVariable.Type.Dataschema, from, limit,
      sort, order);

    builder.setTotal(variablesDto.getTotal()).setLimit(variablesDto.getLimit()).setFrom(variablesDto.getFrom());

    variablesDto.getVariablesList()
      .forEach(variable -> builder.addVariableHarmonizations(getVariableHarmonizationDto(dataset, variable.getName(), includeSummaries)));

    return builder.build();

  }

  private Mica.DatasetVariablesHarmonizationSummaryDto getVariableHarmonizationsSummaryInternal(String id,
                                                                                                String nameQuery,
                                                                                                int from, int limit, String sort, String order, boolean includeSummaries) {

    checkDatasetAccess(id);
    Mica.DatasetVariablesHarmonizationSummaryDto.Builder builder = Mica.DatasetVariablesHarmonizationSummaryDto.newBuilder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, id);
    Mica.DatasetVariablesDto variablesDto = Strings.isNullOrEmpty(nameQuery)
      ? getDatasetVariableDtos(id, DatasetVariable.Type.Dataschema, from, limit, sort, order)
      : getDatasetVariableDtosWithNameQuery(id, nameQuery, DatasetVariable.Type.Dataschema, from, limit, sort, order);

    String ids = variablesDto.getVariablesList().stream().map(variableDto -> variableDto.getName()).collect(Collectors.joining(","));
    String namesQuery = String.format("in(name,(%s))",ids);

    int hvariablesLimit = limit * (dataset.getStudyTables().size() + dataset.getHarmonizationTables().size());

    // harmonized variables, extract one column per study table
    List<Map<String, DatasetVariable>> harmonizedVariables = Lists.newArrayList();
    builder.setTotal(variablesDto.getTotal()).setLimit(variablesDto.getLimit()).setFrom(variablesDto.getFrom());

    String query = "and(%s,eq(datasetId,%s),eq(studyId,%s),eq(populationId,%s),eq(dceId,%s),eq(opalTableType,%s),eq(source,%s))";
    dataset.getStudyTables().forEach(st -> {
      builder.addStudyTable(dtos.asDto(st, includeSummaries));
      String rql = String.format(query, namesQuery, id, st.getStudyId(), st.getPopulationUId(), st.getDataCollectionEventUId(), DatasetVariable.OpalTableType.Study, st.getSource());
      List<DatasetVariable> datasetVariablesInternal = getDatasetVariablesInternal(rql, 0, hvariablesLimit, sort, order, true);
      harmonizedVariables.add(datasetVariablesInternal.stream().collect(Collectors.toMap(DatasetVariable::getName, v -> v)));
    });
    dataset.getHarmonizationTables().forEach(st -> {
      builder.addHarmonizationStudyTable(dtos.asDto(st, includeSummaries));
      String rql = String.format(query, namesQuery, id, st.getStudyId(), st.getPopulationUId(), st.getDataCollectionEventUId(), DatasetVariable.OpalTableType.Harmonization, st.getSource());
      harmonizedVariables.add(getDatasetVariablesInternal(rql, 0, hvariablesLimit, sort, order, true).stream().collect(Collectors.toMap(DatasetVariable::getName, v -> v)));
    });

    variablesDto.getVariablesList()
      .forEach(variable -> {
        DatasetVariable.IdResolver variableResolver = DatasetVariable.IdResolver
          .from(dataset.getId(), variable.getName(), DatasetVariable.Type.Dataschema);
        Mica.DatasetVariableHarmonizationSummaryDto.Builder summaryBuilder = Mica.DatasetVariableHarmonizationSummaryDto.newBuilder()
          .setDataschemaVariableRef(dtos.asDto(variableResolver))
          .addAllHarmonizedVariables(harmonizedVariables.stream()
            .map(hvarMap -> dtos.asHarmonizedSummaryDto(hvarMap.get(variable.getName())))
            .collect(Collectors.toList()));
        builder.addVariableHarmonizations(summaryBuilder);
      });

    return builder.build();

  }

  private void checkDatasetAccess(String id) {
    subjectAclService.checkAccess("/harmonized-dataset", id);
  }
}
