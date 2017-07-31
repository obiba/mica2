/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.OpalTable;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.dataset.search.VariableIndexer;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Retrieve the {@link org.obiba.mica.dataset.domain.Dataset} from the published dataset index.
 *
 * @param <T>
 */
public abstract class AbstractPublishedDatasetResource<T extends Dataset> {

  private static final Logger log = LoggerFactory.getLogger(AbstractPublishedDatasetResource.class);

  @Inject
  protected ApplicationContext applicationContext;

  @Inject
  protected Dtos dtos;

  @Inject
  protected Client client;

  @Inject
  protected ObjectMapper objectMapper;

  @Inject
  protected OpalService opalService;

  private String locale;

  protected T getDataset(Class<T> clazz, @NotNull String datasetId) throws NoSuchDatasetException {
    QueryBuilder query = QueryBuilders.queryStringQuery(clazz.getSimpleName()).field("className");
    query = QueryBuilders.boolQuery().must(query)
      .must(QueryBuilders.idsQuery(DatasetIndexer.DATASET_TYPE).addIds(datasetId));

    SearchRequestBuilder search = client.prepareSearch() //
      .setIndices(DatasetIndexer.PUBLISHED_DATASET_INDEX) //
      .setTypes(DatasetIndexer.DATASET_TYPE) //
      .setQuery(query);

    log.info("Request /{}/{}", DatasetIndexer.PUBLISHED_DATASET_INDEX, DatasetIndexer.DATASET_TYPE);
    SearchResponse response = search.execute().actionGet();

    if(response.getHits().totalHits() == 0) throw NoSuchDatasetException.withId(datasetId);

    InputStream inputStream = new ByteArrayInputStream(response.getHits().hits()[0].getSourceAsString().getBytes());
    try {
      T rval = objectMapper.readValue(inputStream, clazz);
      log.info("Response /{}/{}", DatasetIndexer.PUBLISHED_DATASET_INDEX, DatasetIndexer.DATASET_TYPE);
      return rval;
    } catch(IOException e) {
      log.error("Failed retrieving {}", clazz.getSimpleName(), e);
      throw NoSuchDatasetException.withId(datasetId);
    }
  }

  protected Mica.DatasetDto getDatasetDto(Class<T> clazz, @NotNull String datasetId) throws NoSuchDatasetException {
    return dtos.asDto(getDataset(clazz, datasetId));
  }

  protected Mica.DatasetVariablesDto getDatasetVariableDtos(@NotNull String queryString, @NotNull String datasetId,
    DatasetVariable.Type type, int from, int limit, @Nullable String sort, @Nullable String order) {
    QueryBuilder query = FilteredQueryBuilder.newBuilder().must("datasetId", datasetId)
      .must("variableType", type.toString()).build(QueryStringBuilder.newBuilder(queryString).build());

    return getDatasetVariableDtosInternal(query, from, limit, sort, order);
  }

  protected Mica.DatasetVariablesDto getDatasetVariableDtos(@NotNull String datasetId, DatasetVariable.Type type, int from,
    int limit, @Nullable String sort, @Nullable String order) {
    QueryBuilder query = FilteredQueryBuilder.newBuilder().must("datasetId", datasetId)
      .must("variableType", type.toString()).build(QueryBuilders.matchAllQuery());

    return getDatasetVariableDtosInternal(query, from, limit, sort, order);
  }

  protected Mica.DatasetVariablesDto getDatasetVariableDtosInternal(QueryBuilder query, int from, int limit, @Nullable String sort,
    @Nullable String order) {

    SearchRequestBuilder search = client.prepareSearch() //
      .setIndices(VariableIndexer.PUBLISHED_VARIABLE_INDEX) //
      .setTypes(VariableIndexer.VARIABLE_TYPE) //
      .setQuery(query) //
      .setFrom(from) //
      .setSize(limit);

    if(sort != null) {
      search.addSort(
        SortBuilders.fieldSort(sort).order(order == null ? SortOrder.ASC : SortOrder.valueOf(order.toUpperCase())));
    }

    log.info("Request /{}/{}", VariableIndexer.PUBLISHED_VARIABLE_INDEX, VariableIndexer.VARIABLE_TYPE);
    SearchResponse response = search.execute().actionGet();

    Mica.DatasetVariablesDto.Builder builder = Mica.DatasetVariablesDto.newBuilder() //
      .setTotal(Long.valueOf(response.getHits().getTotalHits()).intValue()) //
      .setFrom(from) //
      .setLimit(limit);

    List<Taxonomy> taxonomies = getTaxonomies();
    response.getHits().forEach(hit -> {
      InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
      try {
        builder.addVariables(dtos.asDto(objectMapper.readValue(inputStream, DatasetVariable.class), taxonomies));
      } catch(IOException e) {
        log.error("Failed retrieving {}", DatasetVariable.class.getSimpleName(), e);
      }
    });

    log.info("Response /{}/{}", VariableIndexer.PUBLISHED_VARIABLE_INDEX, VariableIndexer.VARIABLE_TYPE);

    return builder.build();
  }

  protected Mica.DatasetVariableHarmonizationDto getVariableHarmonizationDto(HarmonizationDataset dataset,
    String variableName) {
    DatasetVariable.IdResolver variableResolver = DatasetVariable.IdResolver
      .from(dataset.getId(), variableName, DatasetVariable.Type.Dataschema);
    Mica.DatasetVariableHarmonizationDto.Builder builder = Mica.DatasetVariableHarmonizationDto.newBuilder();
    builder.setResolver(dtos.asDto(variableResolver));

    dataset.getAllOpalTables().forEach(table -> {
      try {
        builder.addDatasetVariableSummaries(
          getDatasetVariableSummaryDto(dataset.getId(), variableResolver.getName(), DatasetVariable.Type.Harmonized,
            table));
      } catch(NoSuchVariableException e) {
        log.debug("ignore (case the study has not implemented this dataschema variable)", e);
      }
    });

    return builder.build();
  }

  protected DatasetVariable getDatasetVariable(@NotNull String datasetId, @NotNull String variableName,
    DatasetVariable.Type variableType, OpalTable opalTable) {
    String studyId = opalTable != null && opalTable instanceof BaseStudyTable ? ((BaseStudyTable) opalTable).getStudyId() : null;
    String project = opalTable != null ? opalTable.getProject() : null;
    String table = opalTable != null ? opalTable.getTable() : null;
    String tableType = opalTable == null ? null :
      opalTable instanceof  StudyTable
        ? DatasetVariable.OPAL_STUDY_TABLE_PREFIX
        : DatasetVariable.OPAL_HARMONIZATION_TABLE_PREFIX;

    return getDatasetVariable(datasetId, variableName, variableType, studyId, project, table, tableType);
  }

  /**
   * Look for a variable of a dataset in the published dataset index or the published study index depending on
   * whether a study ID is provided for resolving variable's parent.
   *
   * @param datasetId
   * @param variableName
   * @param studyId
   * @param project
   * @param table
   * @return
   * @throws NoSuchVariableException
   */
  protected DatasetVariable getDatasetVariable(@NotNull String datasetId, @NotNull String variableName,
    DatasetVariable.Type variableType, @Nullable String studyId, @Nullable String project, @Nullable String table,
    @Nullable String tableType)
    throws NoSuchVariableException {

    String variableId = DatasetVariable.IdResolver
      .encode(datasetId, variableName, variableType, studyId, project, table, tableType);

    if(variableType.equals(DatasetVariable.Type.Harmonized)) {
      return getHarmonizedDatasetVariable(datasetId, variableId, variableName);
    }

    return getDatasetVariableInternal(VariableIndexer.VARIABLE_TYPE, variableId, variableName);
  }

  protected Mica.DatasetVariableDto getDatasetVariableDto(@NotNull String datasetId, @NotNull String variableName,
    DatasetVariable.Type variableType) {
    return getDatasetVariableDto(datasetId, variableName, variableType, null);
  }

  protected Mica.DatasetVariableDto getDatasetVariableDto(@NotNull String datasetId, @NotNull String variableName,
    DatasetVariable.Type variableType, @Nullable OpalTable opalTable) {
    return dtos
      .asDto(getDatasetVariable(datasetId, variableName, variableType, opalTable), getTaxonomies(),
        getLocale());
  }

  protected Mica.DatasetVariableDto getDatasetVariableDto(@NotNull String datasetId, @NotNull String variableName,
    DatasetVariable.Type variableType, @Nullable String studyId, @Nullable String project, @Nullable String table,
    @Nullable String tableType) {
    return dtos.asDto(getDatasetVariable(datasetId, variableName, variableType, studyId, project, table, tableType),
      getTaxonomies(), getLocale());
  }

  protected Mica.DatasetVariableSummaryDto getDatasetVariableSummaryDto(@NotNull String datasetId,
    @NotNull String variableName, DatasetVariable.Type variableType, @Nullable OpalTable opalTable) {
    DatasetVariable variable = getDatasetVariable(datasetId, variableName, variableType, opalTable);
    return dtos.asSummaryDto(variable, opalTable, true);
  }

  @NotNull
  protected List<Taxonomy> getTaxonomies() {
    List<Taxonomy> taxonomies = null;
    try {
      taxonomies = opalService.getTaxonomies();
    } catch(Exception e) {
      // ignore
    }
    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }

  private DatasetVariable getHarmonizedDatasetVariable(String datasetId, String variableId, String variableName) {
    String dataSchemaVariableId = DatasetVariable.IdResolver
      .encode(datasetId, variableName, DatasetVariable.Type.Dataschema, null, null, null, null);
    DatasetVariable harmonizedDatasetVariable = getDatasetVariableInternal(VariableIndexer.HARMONIZED_VARIABLE_TYPE,
      variableId, variableName);
    DatasetVariable dataSchemaVariable = getDatasetVariableInternal(VariableIndexer.VARIABLE_TYPE,
      dataSchemaVariableId, variableName);

    dataSchemaVariable.getAttributes().asAttributeList().forEach(a -> {
      if(!a.getName().startsWith("Mlstr_harmo")) harmonizedDatasetVariable.addAttribute(a);
    });

    return harmonizedDatasetVariable;
  }

  private DatasetVariable getDatasetVariableInternal(String indexType, String variableId, String variableName) {
    QueryBuilder query = QueryBuilders.idsQuery(indexType).addIds(variableId);

    SearchRequestBuilder search = client.prepareSearch() //
      .setIndices(VariableIndexer.PUBLISHED_VARIABLE_INDEX) //
      .setTypes(indexType) //
      .setQuery(query);

    log.debug("Request: {}", search.toString());
    SearchResponse response = search.execute().actionGet();

    if(response.getHits().totalHits() == 0) throw new NoSuchVariableException(variableName);

    InputStream inputStream = new ByteArrayInputStream(response.getHits().hits()[0].getSourceAsString().getBytes());
    try {
      return objectMapper.readValue(inputStream, DatasetVariable.class);
    } catch(IOException e) {
      log.error("Failed retrieving {}", DatasetVariable.class.getSimpleName(), e);
      throw new NoSuchVariableException(variableName);
    }
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String value) {
    locale = value;
  }

  protected static class QueryStringBuilder {

    private final QueryStringQueryBuilder builder;

    private QueryStringBuilder(String queryString) {
      builder = QueryBuilders.queryStringQuery(queryString);
    }

    public static QueryStringBuilder newBuilder(String queryString) {
      return new QueryStringBuilder(queryString);
    }

    public QueryBuilder build() {
      Stream.of(VariableIndexer.ANALYZED_FIELDS)
        .forEach(f -> builder.field(f + ".analyzed"));
      Stream.of(VariableIndexer.LOCALIZED_ANALYZED_FIELDS)
        .forEach(f -> builder.field("attributes." + f + ".*.analyzed"));

      return builder;
    }
  }

  protected static class FilteredQueryBuilder {
    private BoolQueryBuilder boolFilter = QueryBuilders.boolQuery();

    public static FilteredQueryBuilder newBuilder() {
      return new FilteredQueryBuilder();
    }

    public FilteredQueryBuilder must(String field, String value) {
      boolFilter.must(QueryBuilders.termQuery(field, value));
      return this;
    }

    public QueryBuilder build(QueryBuilder query) {
      return QueryBuilders.boolQuery().must(query).must(boolFilter);
    }
  }
}
