/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.service.TaxonomiesService;
import org.obiba.mica.search.queries.DatasetQuery;
import org.obiba.mica.search.queries.DocumentQueryInterface;
import org.obiba.mica.search.queries.NetworkQuery;
import org.obiba.mica.search.queries.StudyQuery;
import org.obiba.mica.search.queries.VariableQuery;
import org.obiba.mica.spi.search.CountStatsData;
import org.obiba.mica.spi.search.QueryMode;
import org.obiba.mica.spi.search.QueryScope;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.support.JoinQuery;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.mica.web.model.MicaSearch.AggregationResultDto;
import org.obiba.mica.web.model.MicaSearch.JoinQueryResultDto;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.obiba.mica.spi.search.QueryScope.AGGREGATION;
import static org.obiba.mica.spi.search.QueryScope.DETAIL;
import static org.obiba.mica.spi.search.QueryScope.DIGEST;

@Component
@Scope("request")
public class JoinQueryExecutor {

  private static final Logger log = LoggerFactory.getLogger(JoinQueryExecutor.class);

  @Inject
  @Qualifier("esJoinQueriesSemaphore")
  private Semaphore esJoinQueriesSemaphore;


  @Value("${elasticsearch.concurrentJoinQueriesWaitTimeout:30000}")
  private long concurrentJoinQueriesWaitTimeout;

  @Inject
  private TaxonomiesService taxonomiesService;

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
  public JoinQueryResultDto queryWithoutCountStats(QueryType type, JoinQuery joinQuery) {
    return query(type, joinQuery, null, DETAIL, QueryMode.SEARCH);
  }

  @Timed
  public JoinQueryResultDto query(QueryType type, JoinQuery joinQuery) {
    return query(type, joinQuery, CountStatsData.newBuilder(), DETAIL, QueryMode.SEARCH);
  }

  @Timed
  public JoinQueryResultDto queryCoverage(JoinQuery joinQuery) {
    return query(QueryType.VARIABLE, joinQuery, null, DIGEST, QueryMode.COVERAGE);
  }

  private JoinQueryResultDto query(QueryType type, JoinQuery joinQuery,
                                   CountStatsData.Builder countBuilder, QueryScope scope, QueryMode mode) {

    boolean havePermit = false;
    try {
      havePermit = acquireSemaphorePermit();
      if (havePermit)
        return unsafeQuery(type, joinQuery, countBuilder, scope, mode);
      else
        throw new UncheckedTimeoutException("Too many queries in a short time. Please retry later.");
    } finally {
      if (havePermit)
        releaseSemaphorePermit();
    }
  }

  private boolean acquireSemaphorePermit() {
    try {
      return esJoinQueriesSemaphore.tryAcquire(concurrentJoinQueriesWaitTimeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  private void releaseSemaphorePermit() {
    esJoinQueriesSemaphore.release();
  }

  private JoinQueryResultDto unsafeQuery(QueryType type, JoinQuery joinQuery,
                                         CountStatsData.Builder countBuilder, QueryScope scope, QueryMode mode) {
    log.debug("Start query");
    initializeQueries(joinQuery, mode);
    return doQueries(type, joinQuery, countBuilder, scope);
  }

  private JoinQueryResultDto doQueries(QueryType type, JoinQuery joinQuery,
                                       CountStatsData.Builder countBuilder, QueryScope scope) {
    boolean queriesHaveFilters = Stream.of(variableQuery, datasetQuery, studyQuery, networkQuery)
        .anyMatch(DocumentQueryInterface::hasQueryBuilder);

    if (queriesHaveFilters) {
      DocumentQueryIdProvider datasetIdProvider = new DocumentQueryIdProvider();
      variableQuery.setDatasetIdProvider(datasetIdProvider);
      datasetQuery.setDatasetIdProvider(datasetIdProvider);

      List<String> joinedIds = executeJoin(type);
      CountStatsData countStats = countBuilder != null ? getCountStatsData(type) : null;

      if ((joinQuery.searchOnNetworksOnly() && QueryType.NETWORK.equals(type)) || (joinedIds != null && joinedIds.size() > 0)) {
        getDocumentQuery(type).query((joinQuery.searchOnNetworksOnly() && QueryType.NETWORK.equals(type)) ? Collections.emptyList() : joinedIds, countStats, scope);
      }
    } else {
      execute(type, scope, countBuilder);
    }

    log.debug("Building result");
    JoinQueryResultDto resultDto = buildQueryResult(joinQuery);
    log.debug("Finished query");

    return resultDto;
  }

  private void initializeQueries(JoinQuery joinQuery, QueryMode mode) {
    String locale = joinQuery.getLocale();

    variableQuery.initialize(joinQuery.getVariableQuery(), locale, mode);
    datasetQuery.initialize(joinQuery.getDatasetQuery(), locale, mode);
    studyQuery.initialize(joinQuery.getStudyQuery(), locale, mode);
    networkQuery.initialize(joinQuery.getNetworkQuery(), locale, mode);
  }

  private JoinQueryResultDto buildQueryResult(JoinQuery joinQueryDto) {
    JoinQueryResultDto.Builder builder = JoinQueryResultDto.newBuilder();

    builder.setVariableResultDto(joinQueryDto.isWithFacets()
        ? addAggregationTitles(variableQuery.getResultQuery(),
        ImmutableList.<Taxonomy>builder().add(taxonomiesService.getVariableTaxonomy())
            .addAll(taxonomiesService.getVariableTaxonomies()).build(), aggregationPostProcessor())
        : removeAggregations(variableQuery.getResultQuery()));

    builder.setDatasetResultDto(joinQueryDto.isWithFacets()
        ? addAggregationTitles(datasetQuery.getResultQuery(), Lists.newArrayList(taxonomiesService.getDatasetTaxonomy()),
        null)
        : removeAggregations(datasetQuery.getResultQuery()));

    builder.setStudyResultDto(joinQueryDto.isWithFacets()
        ? addAggregationTitles(studyQuery.getResultQuery(), Lists.newArrayList(taxonomiesService.getStudyTaxonomy()),
        null)
        : removeAggregations(studyQuery.getResultQuery()));

    builder.setNetworkResultDto(joinQueryDto.isWithFacets()
        ? addAggregationTitles(networkQuery.getResultQuery(), Lists.newArrayList(taxonomiesService.getNetworkTaxonomy()),
        null)
        : removeAggregations(networkQuery.getResultQuery()));

    return builder.build();
  }

  private MicaSearch.QueryResultDto removeAggregations(MicaSearch.QueryResultDto queryResultDto) {
    MicaSearch.QueryResultDto.Builder builder;
    if (queryResultDto != null) {
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

    if (queryResultDto != null) {
      MicaSearch.QueryResultDto.Builder builder = MicaSearch.QueryResultDto.newBuilder().mergeFrom(queryResultDto);
      List<AggregationResultDto.Builder> builders = ImmutableList.copyOf(builder.getAggsBuilderList());
      builder.clearAggs();

      List<AggregationResultDto> aggregationResultDtos = builders.stream().map(b -> {
        taxonomies.forEach(taxonomy -> taxonomy.getVocabularies().forEach(voc -> {
          if (b.getAggregation().equals(getVocabularyAggregationName(voc))) {
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
      Map<String, AggregationResultDto.Builder> buildres = taxonomiesService.getVariableTaxonomies().stream()
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
        if (matcher.find()) {
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
          .sorted(Comparator.comparing(AggregationResultDto.Builder::getAggregation)) //
          .map(MicaSearch.AggregationResultDto.Builder::build) //
          .collect(Collectors.toList())); //

      return newList;
    };
  }

  private void execute(QueryType type, QueryScope scope, CountStatsData.Builder countBuilder) {

    CountStatsData countStats;

    switch (type) {
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

  private List<String> executeJoin(QueryType type) {
    List<String> joinedIds = null;

    switch (type) {
      case VARIABLE:
        joinedIds = execute(variableQuery, studyQuery, datasetQuery, networkQuery);
        break;
      case DATASET:
        joinedIds = execute(datasetQuery, variableQuery, studyQuery, networkQuery);
        break;
      case STUDY:
        joinedIds = datasetQuery.hasIdCriteria()
            ? execute(studyQuery, datasetQuery, variableQuery, networkQuery)
            : execute(studyQuery, variableQuery, datasetQuery, networkQuery);
        break;
      case NETWORK:
        joinedIds = datasetQuery.hasIdCriteria()
            ? execute(networkQuery, datasetQuery, variableQuery, studyQuery)
            : execute(networkQuery, variableQuery, datasetQuery, studyQuery);
        break;
    }

    return joinedIds;
  }

  private DocumentQueryInterface getDocumentQuery(QueryType type) {
    switch (type) {
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
    switch (type) {
      case DATASET:
        countStats = CountStatsData.newBuilder().variables(variableQuery.getDatasetCounts())
            .studyVariables(variableQuery.getStudyVariableByDatasetCounts())
            .dataschemaVariables(variableQuery.getDataschemaVariableByDatasetCounts())
            .studies(studyQuery.getStudyCounts())
            .individualStudies(studyQuery.getIndividualStudyCounts())
            .harmonizationStudies(studyQuery.getHarmonizationStudyCounts())
            .networksMap(networkQuery.getStudyCountsByNetwork()).build();
        break;
      case STUDY:
        countStats = CountStatsData.newBuilder().variables(variableQuery.getStudyCounts()) //
            .studyVariables(variableQuery.getStudyVariableByStudyCounts()) //
            .dataschemaVariables(variableQuery.getDataschemaVariableByStudyCounts()) //
            .studyDatasets(datasetQuery.getStudyCounts()) //
            .harmonizationDatasets(datasetQuery.getHarmonizationStudyCounts()) //
            .networks(networkQuery.getStudyCounts()) //
            .build();
        break;
      case NETWORK:
        countStats = CountStatsData.newBuilder().variables(variableQuery.getDatasetCounts())
            .datasetsMap(datasetQuery.getStudyCountsByDataset())
            .individualStudies(studyQuery.getIndividualStudyCounts())
            .harmonizationStudies(studyQuery.getHarmonizationStudyCounts())
            .studies(studyQuery.getStudyCounts()).build();
        break;
    }

    return countStats;
  }

  private List<String> execute(DocumentQueryInterface docQuery, DocumentQueryInterface... subQueries) {
    List<DocumentQueryInterface> queries = Arrays.stream(subQueries)
        .filter(DocumentQueryInterface::hasQueryBuilder)
        .collect(Collectors.toList());

    List<String> studyIds = null;
    List<String> docQueryStudyIds = null;
    if (queries.size() > 0) studyIds = queryStudyIds(queries);
    if (studyIds == null || studyIds.size() > 0) docQueryStudyIds = docQuery.queryStudyIds(studyIds);

    List<String> aggStudyIds = docQuery.hasQueryBuilder() && docQueryStudyIds != null ? joinStudyIds(studyIds,
        docQueryStudyIds) : studyIds;

    if (aggStudyIds == null || aggStudyIds.size() > 0) {
      queryAggregations(aggStudyIds, subQueries);
    }

    return aggStudyIds;
  }

  private List<String> joinStudyIds(List<String> studyIds, List<String> joinedStudyIds) {
    if (studyIds != null) {
      joinedStudyIds.retainAll(studyIds);
    }

    return joinedStudyIds;
  }

  private void queryAggregations(List<String> studyIds, DocumentQueryInterface... queries) {
    for (DocumentQueryInterface query : queries) {
      query.query(studyIds, null, AGGREGATION);
    }
  }

  private List<String> queryStudyIds(List<DocumentQueryInterface> queries) {
    List<String> studyIds = queries.get(0).queryStudyIds();

    queries.stream().skip(1).forEach(query -> {
      if (studyIds.size() > 0) {
        studyIds.retainAll(query.queryStudyIds());
      }
    });

    return studyIds;
  }
}
