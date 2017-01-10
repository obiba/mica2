/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.service.TaxonomyService;
import org.obiba.mica.search.queries.AbstractDocumentQuery;
import org.obiba.mica.search.queries.AbstractDocumentQuery.Mode;
import org.obiba.mica.search.queries.DatasetQuery;
import org.obiba.mica.search.queries.JoinQueryWrapper;
import org.obiba.mica.search.queries.NetworkQuery;
import org.obiba.mica.search.queries.StudyQuery;
import org.obiba.mica.search.queries.VariableQuery;
import org.obiba.mica.search.queries.protobuf.JoinQueryDtoWrapper;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.mica.web.model.MicaSearch.AggregationResultDto;
import org.obiba.mica.web.model.MicaSearch.JoinQueryDto;
import org.obiba.mica.web.model.MicaSearch.JoinQueryResultDto;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import static org.obiba.mica.search.queries.AbstractDocumentQuery.Scope.DETAIL;
import static org.obiba.mica.search.queries.AbstractDocumentQuery.Scope.DIGEST;

@Component
@Scope("request")
public class JoinQueryExecutor {

  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentQuery.class);

  @Inject
  @Qualifier("esJoinQueriesSemaphore")
  private Semaphore esJoinQueriesSemaphore;

  @Value("${elasticsearch.concurrentJoinQueriesWaitTimeout:30000}")
  private long concurrentJoinQueriesWaitTimeout;

  public enum QueryType {
    VARIABLE,
    DATASET,
    STUDY,
    NETWORK
  }

  @Inject
  private TaxonomyService taxonomyService;

  @Inject
  private VariableQuery variableQuery;

  @Inject
  private DatasetQuery datasetQuery;

  @Inject
  private StudyQuery studyQuery;

  @Inject
  private NetworkQuery networkQuery;

  @Inject
  private Dtos dtos;

  @Timed
  public JoinQueryResultDto query(QueryType type, JoinQueryWrapper joinQueryWrapper) throws IOException {
    return query(type, joinQueryWrapper, CountStatsData.newBuilder(), DETAIL, Mode.SEARCH);
  }

  @Timed
  public JoinQueryResultDto queryCoverage(JoinQueryWrapper joinQueryWrapper) throws IOException {
    return query(QueryType.VARIABLE, joinQueryWrapper, null, DIGEST, Mode.COVERAGE);
  }

  @Timed
  public JoinQueryResultDto listQuery(QueryType type, MicaSearch.QueryDto queryDto, String locale) throws IOException {
    JoinQueryDto joinQueryDto = createJoinQueryByType(type, queryDto).toBuilder().setWithFacets(false).setLocale(locale)
        .build();

    JoinQueryWrapper joinQueryWrapper = new JoinQueryDtoWrapper(joinQueryDto);
    initializeQueries(joinQueryWrapper, Mode.LIST);

    execute(type, DETAIL, CountStatsData.newBuilder());

    JoinQueryResultDto.Builder builder = JoinQueryResultDto.newBuilder();
    if(variableQuery.getResultQuery() != null) builder.setVariableResultDto(variableQuery.getResultQuery());
    if(datasetQuery.getResultQuery() != null) builder.setDatasetResultDto(datasetQuery.getResultQuery());
    if(studyQuery.getResultQuery() != null) builder.setStudyResultDto(studyQuery.getResultQuery());
    if(networkQuery.getResultQuery() != null) builder.setNetworkResultDto(networkQuery.getResultQuery());

    return builder.build();
  }

  private JoinQueryDto createJoinQueryByType(QueryType type, MicaSearch.QueryDto queryDto) {
    JoinQueryDto.Builder builder = JoinQueryDto.newBuilder();

    switch(type) {
      case VARIABLE:
        builder.setVariableQueryDto(queryDto).build();
        break;
      case DATASET:
        builder.setDatasetQueryDto(queryDto).build();
        break;
      case STUDY:
        builder.setStudyQueryDto(queryDto).build();
        break;
      case NETWORK:
        builder.setNetworkQueryDto(queryDto);
        break;
    }

    return builder.build();
  }

  private JoinQueryResultDto query(QueryType type, JoinQueryWrapper joinQueryWrapper,
                                   CountStatsData.Builder countBuilder, AbstractDocumentQuery.Scope scope, AbstractDocumentQuery.Mode mode)
    throws IOException {

    boolean havePermit = false;
    try {
      havePermit = acquireSemaphorePermit();
      if (havePermit)
        return unsafeQuery(type, joinQueryWrapper, countBuilder, scope, mode);
      else
        throw new UncheckedTimeoutException("Too many queries in a short time. Please retry later.");
    } finally {
      if (havePermit)
        releaseSemaphorePermit();
    }
  }

  private boolean acquireSemaphorePermit() throws IOException {
    try {
      return esJoinQueriesSemaphore.tryAcquire(concurrentJoinQueriesWaitTimeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  private void releaseSemaphorePermit() {
    esJoinQueriesSemaphore.release();
  }

  private JoinQueryResultDto unsafeQuery(QueryType type, JoinQueryWrapper joinQueryWrapper,
      CountStatsData.Builder countBuilder, AbstractDocumentQuery.Scope scope, AbstractDocumentQuery.Mode mode)
      throws IOException {
    log.debug("Start query");
    initializeQueries(joinQueryWrapper, mode);
    return doQueries(type, joinQueryWrapper, countBuilder, scope);
  }

  private JoinQueryResultDto doQueries(QueryType type, JoinQueryWrapper joinQueryWrapper,
      CountStatsData.Builder countBuilder, AbstractDocumentQuery.Scope scope) throws IOException {
    boolean queriesHaveFilters = Stream.of(variableQuery, datasetQuery, studyQuery, networkQuery)
        .filter(AbstractDocumentQuery::hasQueryBuilder).findFirst().isPresent();

    if(queriesHaveFilters) {
      DatasetIdProvider datasetIdProvider = new DatasetIdProvider();
      variableQuery.setDatasetIdProvider(datasetIdProvider);
      datasetQuery.setDatasetIdProvider(datasetIdProvider);

      List<String> joinedIds = executeJoin(type);
      CountStatsData countStats = countBuilder != null ? getCountStatsData(type) : null;

      if(joinedIds != null && joinedIds.size() > 0) {
        if (type == QueryType.DATASET) {
          // clear previously set datasetIds by resetting shared datasetIdProvider's datasetIds list
          datasetIdProvider.resetDatasetIds();
          queryAggregations(joinedIds, datasetQuery, variableQuery);
        }
        getDocumentQuery(type).query(joinedIds, countStats, scope);
        // need to update dataset and variable and redo agg query
        if(type == QueryType.VARIABLE) {
          datasetQuery.query(joinedIds, null, DIGEST);
        }
      }
    } else {
      execute(type, scope, countBuilder);
    }

    log.debug("Building result");
    JoinQueryResultDto resultDto = buildQueryResult(joinQueryWrapper);
    log.debug("Finished query");

    return resultDto;
  }

  private void initializeQueries(JoinQueryWrapper joinQueryWrapper, AbstractDocumentQuery.Mode mode) {
    String locale = joinQueryWrapper.getLocale();

    variableQuery.initialize(joinQueryWrapper.getVariableQueryWrapper(), locale, mode);
    datasetQuery.initialize(joinQueryWrapper.getDatasetQueryWrapper(), locale, mode);
    studyQuery.initialize(joinQueryWrapper.getStudyQueryWrapper(), locale, mode);
    networkQuery.initialize(joinQueryWrapper.getNetworkQueryWrapper(), locale, mode);
  }

  private JoinQueryResultDto buildQueryResult(JoinQueryWrapper joinQueryDto) {
    JoinQueryResultDto.Builder builder = JoinQueryResultDto.newBuilder();

    builder.setVariableResultDto(joinQueryDto.isWithFacets()
        ? addAggregationTitles(variableQuery.getResultQuery(),
        ImmutableList.<Taxonomy>builder().add(taxonomyService.getVariableTaxonomy())
            .addAll(taxonomyService.getOpalTaxonomies()).build(), aggregationPostProcessor())
        : removeAggregations(variableQuery.getResultQuery()));

    builder.setDatasetResultDto(joinQueryDto.isWithFacets()
        ? addAggregationTitles(datasetQuery.getResultQuery(), Lists.newArrayList(taxonomyService.getDatasetTaxonomy()),
        null)
        : removeAggregations(datasetQuery.getResultQuery()));

    builder.setStudyResultDto(joinQueryDto.isWithFacets()
        ? addAggregationTitles(studyQuery.getResultQuery(), Lists.newArrayList(taxonomyService.getStudyTaxonomy()),
        null)
        : removeAggregations(studyQuery.getResultQuery()));

    builder.setNetworkResultDto(joinQueryDto.isWithFacets()
        ? addAggregationTitles(networkQuery.getResultQuery(), Lists.newArrayList(taxonomyService.getNetworkTaxonomy()),
        null)
        : removeAggregations(networkQuery.getResultQuery()));

    return builder.build();
  }

  private MicaSearch.QueryResultDto removeAggregations(MicaSearch.QueryResultDto queryResultDto) {
    MicaSearch.QueryResultDto.Builder builder;
    if(queryResultDto != null) {
      builder = MicaSearch.QueryResultDto.newBuilder().mergeFrom(queryResultDto);
      builder.clearAggs();
    } else {
      builder = MicaSearch.QueryResultDto.newBuilder();
      builder.setTotalHits(0).setTotalCount(0);
    }
    return builder.build();
  }

  private MicaSearch.QueryResultDto addAggregationTitles(MicaSearch.QueryResultDto queryResultDto,
      List<Taxonomy> taxonomies,
      Function<List<AggregationResultDto>, List<AggregationResultDto>> postProcessor) {

    if(queryResultDto != null) {
      MicaSearch.QueryResultDto.Builder builder = MicaSearch.QueryResultDto.newBuilder().mergeFrom(queryResultDto);
      List<AggregationResultDto.Builder> builders = ImmutableList.copyOf(builder.getAggsBuilderList());
      builder.clearAggs();

      List<AggregationResultDto> aggregationResultDtos = builders.stream().map(b -> {
        taxonomies.forEach(taxonomy -> taxonomy.getVocabularies().forEach(voc -> {
          if(b.getAggregation().equals(getVocabularyAggregationName(voc))) {
            b.addAllTitle(dtos.asDto(LocalizedString.from(voc.getTitle())));
          }
        }));

        return b.build();
      }).collect(Collectors.toList());

      builder.addAllAggs(postProcessor == null ? aggregationResultDtos : postProcessor.apply(aggregationResultDtos));

      return builder.build();
    } else {
      MicaSearch.QueryResultDto.Builder builder = MicaSearch.QueryResultDto.newBuilder();

      taxonomies.forEach(taxonomy -> taxonomy.getVocabularies().forEach(voc -> {
        builder.addAggs(MicaSearch.AggregationResultDto.newBuilder().setAggregation(getVocabularyAggregationName(voc))
            .addAllTitle(dtos.asDto(LocalizedString.from(voc.getTitle()))).build());
      }));

      builder.setTotalHits(0).setTotalCount(0);

      return builder.build();
    }
  }

  private String getVocabularyAggregationName(Vocabulary vocabulary) {
    String alias = vocabulary.getAttributeValue("alias");
    return Strings.isNullOrEmpty(alias) ? vocabulary.getName() : alias;
  }

  protected Function<List<MicaSearch.AggregationResultDto>, List<MicaSearch.AggregationResultDto>> aggregationPostProcessor() {
    return (aggregationResultDtos) -> {
      Map<String, AggregationResultDto.Builder> buildres = taxonomyService.getOpalTaxonomies().stream()
          .collect(Collectors.toMap(Taxonomy::getName, t -> {
            MicaSearch.AggregationResultDto.Builder builder = MicaSearch.AggregationResultDto.newBuilder()
                .setAggregation(t.getName());
            t.getTitle().forEach(
                (k, v) -> builder.addTitle(Mica.LocalizedStringDto.newBuilder().setLang(k).setValue(v).build()));
            return builder;
          }));

      Pattern pattern = Pattern.compile("attributes-(\\w+)__(\\w+)-\\w+$");
      List<MicaSearch.AggregationResultDto> newList = Lists.newArrayList();
      // report only aggregations for which we have results
      List<String> aggregationNames = Lists.newArrayList();
      aggregationResultDtos.forEach(dto -> {
        Matcher matcher = pattern.matcher(dto.getAggregation());
        if(matcher.find()) {
          String taxonomy = matcher.group(1);
          MicaSearch.AggregationResultDto.Builder builder = buildres.get(taxonomy);
          builder.addChildren(dto);
          aggregationNames.add(builder.getAggregation());
        } else {
          newList.add(dto);
        }
      });

      newList.addAll(buildres.values().stream() //
          .filter(b -> aggregationNames.contains(b.getAggregation())) //
          .sorted((b1, b2) -> b1.getAggregation().compareTo(b2.getAggregation())) //
          .map(MicaSearch.AggregationResultDto.Builder::build) //
          .collect(Collectors.toList())); //

      return newList;
    };
  }

  private void execute(QueryType type, AbstractDocumentQuery.Scope scope, CountStatsData.Builder countBuilder)
      throws IOException {

    CountStatsData countStats;

    switch(type) {
      case VARIABLE:
        queryAggregations(null, studyQuery, datasetQuery, networkQuery);
        countStats = countBuilder != null ? getCountStatsData(type) : null;
        variableQuery.query(null, countStats, scope);
        break;
      case DATASET:
        queryAggregations(null, variableQuery, studyQuery, networkQuery);
        countStats = countBuilder != null ? getCountStatsData(type) : null;
        datasetQuery.query(null, countStats, scope);
        break;
      case STUDY:
        queryAggregations(null, variableQuery, datasetQuery, networkQuery);
        countStats = countBuilder != null ? getCountStatsData(type) : null;
        studyQuery.query(null, countStats, scope);
        break;
      case NETWORK:
        queryAggregations(null, variableQuery, datasetQuery, studyQuery);
        countStats = countBuilder != null ? getCountStatsData(type) : null;
        networkQuery.query(null, countStats, scope);
        break;
    }
  }

  private List<String> executeJoin(QueryType type) throws IOException {
    List<String> joinedIds = null;

    switch(type) {
      case VARIABLE:
        joinedIds = execute(variableQuery, studyQuery, datasetQuery, networkQuery);
        break;
      case DATASET:
        joinedIds = execute(datasetQuery, variableQuery, studyQuery, networkQuery);
        break;
      case STUDY:
        joinedIds = datasetQuery.hasPrimaryKeyCriteria()
          ? execute(studyQuery, datasetQuery, variableQuery, networkQuery)
          : execute(studyQuery, variableQuery, datasetQuery, networkQuery);
        break;
      case NETWORK:
        joinedIds = datasetQuery.hasPrimaryKeyCriteria()
            ? execute(networkQuery, datasetQuery, variableQuery, studyQuery)
            : execute(networkQuery, variableQuery, datasetQuery, studyQuery);
        break;
    }

    return joinedIds;
  }

  private AbstractDocumentQuery getDocumentQuery(QueryType type) {
    switch(type) {
      case VARIABLE:
        return variableQuery;
      case DATASET:
        return datasetQuery;
      case STUDY:
        return studyQuery;
      case NETWORK:
        return networkQuery;
    }

    throw new IllegalArgumentException("Illegal query type: " + type);
  }

  private CountStatsData getCountStatsData(QueryType type) {
    CountStatsData countStats = null;
    switch(type) {
      case DATASET:
        countStats = CountStatsData.newBuilder().variables(variableQuery.getDatasetCounts()) //
            .studyVariables(variableQuery.getStudyVariableByDatasetCounts()) //
            .dataschemaVariables(variableQuery.getDataschemaVariableByDatasetCounts()) //
            .studies(studyQuery.getStudyCounts()) //
            .networksMap(networkQuery.getStudyCountsByNetwork()).build();
        break;
      case STUDY:
        countStats = CountStatsData.newBuilder().variables(variableQuery.getStudyCounts()) //
            .studyVariables(variableQuery.getStudyVariableByStudyCounts()) //
            .dataschemaVariables(variableQuery.getDataschemaVariableByStudyCounts()) //
            .studyDatasets(datasetQuery.getStudyCounts()) //
            .harmonizationDatasets(datasetQuery.getHarmonizationStudyCounts()).networks(networkQuery.getStudyCounts())
            .build();
        break;
      case NETWORK:
        countStats = CountStatsData.newBuilder().variables(variableQuery.getDatasetCounts()) //
            .datasetsMap(datasetQuery.getStudyCountsByDataset()) //
            .networkHarmonizationDatasets(datasetQuery.getNetworkCounts()) //
            .networkDataschemaVariables(variableQuery.getNetworkCounts()) //
            .studies(studyQuery.getStudyCounts()).build();
        break;
    }

    return countStats;
  }

  private List<String> execute(AbstractDocumentQuery docQuery, AbstractDocumentQuery... subQueries) throws IOException {
    List<AbstractDocumentQuery> queries = Arrays.asList(subQueries).stream()
        .filter(AbstractDocumentQuery::hasQueryBuilder).collect(Collectors.toList());

    List<String> studyIds = null;
    List<String> docQueryStudyIds = null;
    if(queries.size() > 0) studyIds = queryStudyIds(queries);
    if(studyIds == null || studyIds.size() > 0) docQueryStudyIds = docQuery.queryStudyIds(studyIds);

    List<String> aggStudyIds = docQuery.hasQueryBuilder() && docQueryStudyIds != null ? joinStudyIds(studyIds,
        docQueryStudyIds) : studyIds;

    if(aggStudyIds == null || aggStudyIds.size() > 0) {
      queryAggregations(aggStudyIds, subQueries);
    }

    return aggStudyIds;
  }

  private List<String> joinStudyIds(List<String> studyIds, List<String> joinedStudyIds) {
    if(studyIds != null) {
      joinedStudyIds.retainAll(studyIds);
    }

    return joinedStudyIds;
  }

  private void queryAggregations(List<String> studyIds, AbstractDocumentQuery... queries) throws IOException {
    for(AbstractDocumentQuery query : queries) {
      query.query(studyIds, null, DIGEST);
    }
  }

  private List<String> queryStudyIds(List<AbstractDocumentQuery> queries) throws IOException {
    List<String> studyIds = queries.get(0).queryStudyIds();

    queries.stream().skip(1).forEach(query -> {
      if(studyIds.size() > 0) {
        try {
          studyIds.retainAll(query.queryStudyIds());
        } catch(IOException e) {
          log.error("Failed to query study IDs '{}'", e);
        }

        if(studyIds.isEmpty()) {
          return;
        }
      }
    });

    return studyIds;
  }
}
