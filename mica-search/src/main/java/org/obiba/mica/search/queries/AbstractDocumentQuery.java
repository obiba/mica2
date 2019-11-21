/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.TaxonomyService;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.search.DocumentQueryHelper;
import org.obiba.mica.search.aggregations.AggregationMetaDataResolver;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.CountStatsData;
import org.obiba.mica.spi.search.QueryMode;
import org.obiba.mica.spi.search.QueryScope;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.spi.search.support.AggregationHelper;
import org.obiba.mica.spi.search.support.EmptyQuery;
import org.obiba.mica.spi.search.support.Query;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.obiba.mica.spi.search.QueryMode.COVERAGE;
import static org.obiba.mica.spi.search.QueryScope.DETAIL;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

public abstract class AbstractDocumentQuery implements DocumentQueryInterface {

  @Inject
  protected Searcher searcher;

  @Inject
  protected MicaConfigService micaConfigService;

  @Inject
  protected TaxonomyService taxonomyService;

  @Inject
  protected SubjectAclService subjectAclService;

  @Inject
  private AggregationMetaDataResolver aggregationTitleResolver;

  @Inject
  protected ObjectMapper objectMapper;

  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentQuery.class);

  protected QueryMode mode = QueryMode.SEARCH;

  private Query query;

  QueryResultDto resultDto;

  private String locale;

  public Query getQuery() {
    return query;
  }

  public void setQuery(Query query) {
    this.query = query;
  }

  public boolean isQueryNotEmpty() {
    return !query.isEmpty();
  }

  public boolean hasQueryBuilder() {
    return query.hasQueryBuilder();
  }

  public void initialize(@Nullable Query query, String locale, QueryMode mode) {
    this.mode = mode;
    this.locale = locale;
    this.query = query == null ? new EmptyQuery() : query;
    this.query.ensureAggregationBuckets(getAdditionalAggregationBuckets());
    resultDto = null;
  }

  public QueryResultDto getResultQuery() {
    return resultDto;
  }

  public abstract String getSearchIndex();

  public abstract String getSearchType();

  /**
   * If access check apply, get the corresponding filter.
   */
  @Nullable
  protected Searcher.IdFilter getAccessibleIdFilter() {
    return null;
  }


  protected List<String> getAdditionalAggregationBuckets() {
    return Lists.newArrayList();
  }

  protected boolean isOpenAccess() {
    return micaConfigService.getConfig().isOpenAccess();
  }

  abstract protected Taxonomy getTaxonomy();

  /**
   * Verifies if the query has any criteria on the primary key (ID).
   *
   * @return true/false
   */
  public boolean hasIdCriteria() {
    return query.hasIdCriteria();
  }

  protected Query updateWithJoinKeyQuery(Query query) {
    return query;
  }

  protected DocumentQueryJoinKeys processJoinKeys(Searcher.DocumentResults results) {
    return DocumentQueryHelper.processStudyJoinKey(results);
  }

  protected List<AggregationMetaDataProvider> getAggregationMetaDataProviders() {
    return Lists.newArrayList();
  }

  List getPublishedDocumentsFromHitsByClassName(Searcher.DocumentResults results, Class defaultClass) {
    List published = new ArrayList();
    results.getDocuments().stream()
        .filter(result -> result.hasSource())
        .forEach(result -> {
          try {
            Class clazz = Class.forName(defaultClass.getPackage().getName() + "." + result.getClassName());
            published.add(objectMapper.convertValue(result.getSource(), clazz != null ? clazz : defaultClass));
          } catch (ClassNotFoundException e) {
            log.error("Mandatory field className is not in the source {}", e);
          }
        });
    return published;
  }

  @Nullable
  protected Properties getAggregationsProperties(List<String> filter) {
    return null;
  }

  Properties getAggregationsProperties(List<String> filter, Taxonomy taxonomy) {
    Properties properties = new Properties();
    if (mode != QueryMode.LIST && filter != null && !filter.isEmpty()) {
      List<Pattern> patterns = filter.stream().map(Pattern::compile).collect(Collectors.toList());
      taxonomy.getVocabularies().forEach(vocabulary -> {
        String field = vocabulary.getAttributes().containsKey("field")
            ? vocabulary.getAttributeValue("field")
            : vocabulary.getName().replace('-', '.');

        if (patterns.isEmpty() || patterns.stream().anyMatch(p -> p.matcher(field).matches())) {
          boolean multipleTypes = null != properties.get(field);
          properties.put(field, "");
          String type = vocabulary.getAttributeValue("type");
          if ("integer".equals(type) || "decimal".equals(type)) {
            if ("true".equals(vocabulary.getAttributeValue("range"))) {
              addProperty(properties, field + AggregationHelper.TYPE, AggregationHelper.AGG_RANGE, multipleTypes);
              properties.put(field + AggregationHelper.RANGES,
                  vocabulary.getTerms().stream().map(TaxonomyEntity::getName).collect(Collectors.joining(",")));
            } else {
              addProperty(properties, field + AggregationHelper.TYPE, AggregationHelper.AGG_STATS, multipleTypes);
            }
          }
          String alias = vocabulary.getAttributeValue("alias");
          if (!Strings.isNullOrEmpty(alias)) {
            addProperty(properties, field + AggregationHelper.ALIAS, alias, multipleTypes);
          }
        }
      });
    }
    return properties;
  }

  private void addProperty(Properties properties, String property, String value, boolean multipleTypes) {
    if (multipleTypes) {
      String currentTypes = properties.get(property).toString();
      properties.put(property, currentTypes + "," + value);
    } else {
      properties.put(property, value);
    }
  }

  private Properties getFilteredAggregationsProperties() {
    Properties properties = getAggregationsProperties(query.getAggregations());

    // make sure the buckets are part of the aggregations
    if (properties != null) {
      Properties subAggProperties =
        getAggregationsProperties(
          query.getAggregationBuckets().stream()
            .filter(b -> !properties.containsKey(b))
            .collect(Collectors.toList()), getTaxonomy()
        );

      properties.putAll(subAggProperties);
    }

    return properties;
  }

  /**
   * Executes query to extract study IDs from the aggregation results
   *
   * @return List of study IDs
   * @throws IOException
   */
  public List<String> queryStudyIds() {
    return queryStudyIds(query);
  }

  /**
   * Used on a document query to extract studsy IDs without details
   *
   * @param studyIds
   * @return
   * @throws IOException
   */
  public List<String> queryStudyIds(List<String> studyIds) {
    return queryStudyIds(newStudyIdQuery(studyIds));
  }

  private List<String> queryStudyIds(Query localQuery) {
    if (localQuery == null) return null;
    if ("Network".equals(getSearchType()) && !micaConfigService.getConfig().isNetworkEnabled()) return null;

    Query updatedQuery = updateWithJoinKeyQuery(localQuery);
    Searcher.IdFilter idFilter = getAccessibleIdFilter();

    Searcher.DocumentResults results = searcher.aggregate(getSearchIndex(), getSearchType(), updatedQuery, getJoinFieldsAsProperties(), idFilter);
    DocumentQueryJoinKeys joinKeys = processJoinKeys(results);
    return joinKeys.studyIds.stream().distinct().collect(Collectors.toList());
  }

  private Properties getJoinFieldsAsProperties() {
    Properties props = new Properties();

    try {
      props.load(new StringReader(getAggJoinFields().stream().reduce((t, s) -> t + "=\r" + s).get()));
    } catch (IOException e) {
      log.error("Failed to create properties from query join fields: {}", e);
    }

    return props;
  }

  /**
   * Executes a filtered query to retrieve documents and aggregations.
   */
  public void query(List<String> studyIds, CountStatsData counts, QueryScope scope) {
    Query tempQuery = newStudyIdQuery(studyIds);

    if (mode == COVERAGE) {
      executeCoverage(tempQuery);
    } else {
      execute(tempQuery, scope, counts);
    }
  }

  /**
   * Executes a query to retrieve documents and aggregations.
   */
  private void execute(Query query, QueryScope scope, CountStatsData counts) {
    if (query == null) return;
    if ("Network".equals(getSearchType()) && !micaConfigService.getConfig().isNetworkEnabled()) return;

    aggregationTitleResolver.registerProviders(getAggregationMetaDataProviders());
    aggregationTitleResolver.refresh();

    try {
      Searcher.DocumentResults results = searcher.query(getSearchIndex(), getSearchType(), query, scope, getMandatorySourceFields(), getFilteredAggregationsProperties(), getAccessibleIdFilter());
      QueryResultDto.Builder builder = QueryResultDto.newBuilder().setTotalHits((int) results.getTotal());
      if (scope == DETAIL) processHits(builder, results, scope, counts);
      processAggregations(builder, results);
      resultDto = builder.build();
      getResponseStudyIds(resultDto.getAggsList());
    } catch (Exception e) {
      log.error("Query execution error", e);
    } finally {
      aggregationTitleResolver.unregisterProviders(getAggregationMetaDataProviders());
    }
  }

  /**
   * Executes a query to retrieve documents and aggregations for coverage.
   */
  private void executeCoverage(Query query) {
    if (query == null) return;
    if ("Network".equals(getSearchType()) && !micaConfigService.getConfig().isNetworkEnabled()) return;

    aggregationTitleResolver.registerProviders(getAggregationMetaDataProviders());
    aggregationTitleResolver.refresh();

    try {
      Searcher.DocumentResults results = searcher.cover(getSearchIndex(), getSearchType(), query, getFilteredAggregationsProperties(), getAccessibleIdFilter());
      QueryResultDto.Builder builder = QueryResultDto.newBuilder().setTotalHits((int) results.getTotal());
      processAggregations(builder, results);
      resultDto = builder.build();
      getResponseStudyIds(resultDto.getAggsList());
    } catch (Exception e) {
      log.error("Coverage execution error", e);
    } finally {
      aggregationTitleResolver.unregisterProviders(getAggregationMetaDataProviders());
    }
  }

  protected List<String> getMandatorySourceFields() {
    return Lists.newArrayList();
  }

  /**
   * Creates domain documents DTOs
   *
   * @param builder
   * @param results
   * @param scope
   * @param counts
   * @throws IOException
   */
  protected abstract void processHits(QueryResultDto.Builder builder, Searcher.DocumentResults results, QueryScope scope, CountStatsData counts) throws IOException;

  /**
   * Creates domain aggregation DTOs
   *
   * @param builder
   * @param results
   */
  private void processAggregations(QueryResultDto.Builder builder, Searcher.DocumentResults results) {
    log.debug("start processAggregations");
    EsQueryResultParser parser = EsQueryResultParser.newParser(aggregationTitleResolver, locale);
    builder.addAllAggs(parser.parseAggregations(results.getAggregations()));
    builder.setTotalCount(parser.getTotalCount());
  }

  /**
   * Returns study ID fields (aggregation fields)
   *
   * @return
   */
  protected List<String> getAggJoinFields() {
    return getJoinFields();
  }

  protected abstract List<String> getJoinFields();

  private Query newStudyIdQuery(List<String> studyIds) {
    return isQueryNotEmpty() ? addStudyIdQuery(studyIds) : createStudyIdQuery(studyIds);
  }

  private Query addStudyIdQuery(List<String> studyIds) {
    if (studyIds == null || studyIds.isEmpty()) {
      return query.isEmpty() ? new EmptyQuery() : query;
    }
    return searcher.andQuery(query, createStudyIdQuery(studyIds));
  }

  private Query createStudyIdQuery(List<String> studyIds) {
    if (studyIds == null || studyIds.isEmpty()) {
      return query.isEmpty() ? new EmptyQuery() : query;
    }
    List<String> joinFields = getJoinFields();
    String joinedStudyIds = Joiner.on(",").join(studyIds);
    String rql;
    if (joinFields.size() == 1) {
      rql = String.format("in(%s,(%s))", joinFields.get(0), joinedStudyIds);
    } else {
      rql = String.format("or(%s)", joinFields.stream()
          .map(field -> String.format("in(%s,(%s))", field, joinedStudyIds))
          .collect(Collectors.joining(",")));
    }
    // FIXME from/size may not be appropriate
    //return searcher.makeQuery(String.format("%s,limit(%s,%s)", rql, query.getFrom(), query.getSize()));
    return searcher.makeQuery(rql);
  }

  public abstract Map<String, Integer> getStudyCounts();

  /**
   * Iterate through response aggregations and retrieve the studyIds that were included
   *
   * @param aggDtos
   * @return
   */
  private List<String> getResponseStudyIds(List<MicaSearch.AggregationResultDto> aggDtos) {
    return getResponseDocumentIds(getJoinFields(), aggDtos);
  }

  Map<String, Integer> getDocumentCounts(String joinField) {
    if (resultDto == null) return Maps.newHashMap();
    return getDocumentCountsFilteredByClassName(joinField, null);
  }

  Map<String, Integer> getDocumentCountsFilteredByClassName(String joinField, String className) {
    if (resultDto == null) return Maps.newHashMap();

    return
        resultDto.getAggsList().stream()
            .filter(agg -> joinField.equals(AggregationHelper.unformatName(agg.getAggregation())))
            .map(d -> d.getExtension(MicaSearch.TermsAggregationResultDto.terms)).flatMap(Collection::stream)
            .filter(s -> s.getCount() > 0)
            .filter(s -> Strings.isNullOrEmpty(className) || className.equals(s.getClassName()))
            .collect(Collectors.toMap(
                MicaSearch.TermsAggregationResultDto::getKey,
                MicaSearch.TermsAggregationResultDto::getCount)
            );
  }

  Map<String, Integer> getDocumentBucketCounts(String joinField, String bucketField, String bucketValue) {
    if (resultDto == null) return Maps.newHashMap();
    return resultDto.getAggsList().stream()
        .filter(agg -> bucketField.equals(AggregationHelper.unformatName(agg.getAggregation())))
        .map(d -> d.getExtension(MicaSearch.TermsAggregationResultDto.terms))
        .flatMap(Collection::stream)
        .filter(t -> bucketValue.equals(t.getKey()))
        .map(t -> t.getAggsList())
        .flatMap(Collection::stream)
        .filter(agg -> joinField.equals(AggregationHelper.unformatName(agg.getAggregation())))
        .map(d -> d.getExtension(MicaSearch.TermsAggregationResultDto.terms))
        .flatMap(Collection::stream)
        .filter(s -> s.getCount() > 0)
        .collect(Collectors.toMap(MicaSearch.TermsAggregationResultDto::getKey, term -> term.getCount()));
  }

  List<String> getResponseDocumentIds(List<String> fields, List<MicaSearch.AggregationResultDto> aggDtos) {
    log.debug("start getResponseDocumentIds");
    List<String> ids = aggDtos.stream() //
        .filter(agg -> fields.contains(AggregationHelper.unformatName(agg.getAggregation()))) //
        .map(d -> d.getExtension(MicaSearch.TermsAggregationResultDto.terms)) //
        .flatMap((d) -> d.stream()) //
        .filter(s -> s.getCount() > 0) //
        .map(MicaSearch.TermsAggregationResultDto::getKey) //
        .collect(Collectors.toList()); //
    return ids;
  }

}
