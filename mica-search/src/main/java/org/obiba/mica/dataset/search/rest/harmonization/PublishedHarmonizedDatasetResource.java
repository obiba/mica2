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
import com.google.common.collect.Lists;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
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

  @Inject
  private SubjectAclService subjectAclService;

  /**
   * Get {@link HarmonizationDataset} from published index.
   *
   * @return
   */
  @GET
  @Timed
  public Mica.DatasetDto get(@PathParam("id") String id) {
    checkAccess(id);
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
    checkAccess(id);
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
    checkAccess(id);
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
                                                                                       @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
                                                                                       @QueryParam("sort") @DefaultValue("index") String sort, @QueryParam("order") @DefaultValue("asc") String order) {
    return getVariableHarmonizationsSummaryInternal(id, from, limit, sort, order, true);
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
    checkAccess(id);
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
    checkAccess(id);
    String rql = String.format("and(eq(datasetId,%s),eq(studyId,%s),eq(variableType,%s))", id, studyId, DatasetVariable.Type.Dataschema.toString());
    return getDatasetVariableDtosInternal(rql, from, limit, sort, order);
  }

  @Path("/study/{study}/population/{population}/data-collection-event/{dce}/variable/{variable}")
  public PublishedHarmonizedDatasetVariableResource getVariable(@PathParam("id") String id,
                                                                @PathParam("study") String studyId, @PathParam("population") String populationId, @PathParam("dce") String dceId,
                                                                @PathParam("variable") String variable) {
    checkAccess(id);
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

  private Mica.DatasetVariablesHarmonizationsDto getVariableHarmonizationsInternal(String id,
                                                                                   int from, int limit, String sort, String order, boolean includeSummaries) {

    checkAccess(id);
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
                                                                                                int from, int limit, String sort, String order, boolean includeSummaries) {

    checkAccess(id);
    Mica.DatasetVariablesHarmonizationSummaryDto.Builder builder = Mica.DatasetVariablesHarmonizationSummaryDto.newBuilder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, id);
    Mica.DatasetVariablesDto variablesDto = getDatasetVariableDtos(id, DatasetVariable.Type.Dataschema, from, limit,
      sort, order);

    // harmonized variables, extract one column per study table
    List<Map<String, DatasetVariable>> harmonizedVariables = Lists.newArrayList();
    builder.setTotal(variablesDto.getTotal()).setLimit(variablesDto.getLimit()).setFrom(variablesDto.getFrom());
    String query = "and(eq(datasetId,%s),eq(studyId,%s),eq(populationId,%s),eq(dceId,%s),eq(opalTableType,%s),eq(project,%s),eq(table,%s))";
    dataset.getStudyTables().forEach(st -> {
      builder.addStudyTable(dtos.asDto(st, includeSummaries));
      String rql = String.format(query, id, st.getStudyId(), st.getPopulationUId(), st.getDataCollectionEventUId(), DatasetVariable.OpalTableType.Study, st.getProject(), st.getTable());
      harmonizedVariables.add(getDatasetVariablesInternal(rql, from, limit, sort, order, true).stream().collect(Collectors.toMap(DatasetVariable::getName, v -> v)));
    });
    dataset.getHarmonizationTables().forEach(st -> {
      builder.addHarmonizationStudyTable(dtos.asDto(st, includeSummaries));
      String rql = String.format(query, id, st.getStudyId(), st.getPopulationUId(), st.getDataCollectionEventUId(), DatasetVariable.OpalTableType.Harmonization, st.getProject(), st.getTable());
      harmonizedVariables.add(getDatasetVariablesInternal(rql, from, limit, sort, order, true).stream().collect(Collectors.toMap(DatasetVariable::getName, v -> v)));
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

  private void checkAccess(String id) {
    subjectAclService.checkAccess("/harmonized-dataset", id);
  }
}
