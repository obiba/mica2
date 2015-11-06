/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
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
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import sun.util.locale.LanguageTag;

public abstract class AbstractPublishedDocumentService<T> implements PublishedDocumentService<T> {

  private static final Logger log = LoggerFactory.getLogger(AbstractPublishedDocumentService.class);

  private static final int MAX_SIZE = 99999;

  @Inject
  protected Client client;

  @Inject
  private MicaConfigService micaConfigService;

  public T findById(String id) {
    log.debug("findById {} {}", getClass(), id);
    List<T> results = findByIds(Arrays.asList(id));
    return (results != null && results.size() > 0) ? results.get(0) : null;
  }

  public List<T> findAll() {
    log.debug("findAll {}", getClass());
    return executeQuery(QueryBuilders.matchAllQuery(), 0, MAX_SIZE);
  }

  public List<T> findByIds(List<String> ids) {
    log.debug("findByIds {} {} ids", getClass(), ids.size());
    return executeQueryByIds(buildFilteredQuery(ids), 0, MAX_SIZE, ids);
  }

  @Override
  public Documents<T> find(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String studyId,
    @Nullable String queryString) {
    return find(from, limit, sort, order, studyId, queryString, null);
  }

  @Override
  public Documents<T> find(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String studyId,
    @Nullable String queryString, @Nullable List<String> fields) {
    final QueryStringQueryBuilder query = queryString != null ? QueryBuilders.queryString(queryString) : null;

    if(query != null && fields != null) fields.forEach(f -> query.field(f));

    FilterBuilder filter = null;

    if(studyId != null) {
      filter = filterByStudy(studyId);
    }

    SearchRequestBuilder search = client.prepareSearch() //
      .setIndices(getIndexName()) //
      .setTypes(getType()) //
      .setQuery(query) //
      .setPostFilter(filter) //
      .setFrom(from) //
      .setSize(limit);

    if(sort != null) {
      search.addSort(
        SortBuilders.fieldSort(sort).order(order == null ? SortOrder.ASC : SortOrder.valueOf(order.toUpperCase())));
    }

    log.debug("Request: {}", search.toString());
    SearchResponse response = search.execute().actionGet();

    Documents<T> documents = new Documents<>(Long.valueOf(response.getHits().getTotalHits()).intValue(), from, limit);

    log.debug("found {} hits", response.getHits().getTotalHits());

    response.getHits().forEach(hit -> {
      try {
        documents.add(processHit(hit));
      } catch(IOException e) {
        log.error("Failed processing found hits.", e);
      }
    });

    return documents;
  }

  /**
   * Turns a search hit into document's pojo.
   *
   * @param hit
   * @return
   * @throws IOException
   */
  protected abstract T processHit(SearchHit hit) throws IOException;

  /**
   * Get the index where the search must take place.
   *
   * @return
   */
  protected abstract String getIndexName();

  /**
   * Get the document type name.
   *
   * @return
   */
  protected abstract String getType();

  private List<T> executeQuery(QueryBuilder queryBuilder, int from, int size) {
    return executeQueryInternal(queryBuilder, from, size, null);
  }

  private List<T> executeQueryByIds(QueryBuilder queryBuilder, int from, int size, List<String> ids) {
    return executeQueryInternal(queryBuilder, from, size, ids);
  }

  private List<T> executeQueryInternal(QueryBuilder queryBuilder, int from, int size, List<String> ids) {
    SearchRequestBuilder requestBuilder = client.prepareSearch(getIndexName()) //
      .setTypes(getType()) //
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
      .setQuery(queryBuilder) //
      .setFrom(from) //
      .setSize(size);

    try {
      SearchResponse response = requestBuilder.execute().actionGet();
      SearchHits hits = response.getHits();
      return ids == null || ids.size() != hits.totalHits()
        ? processHits(response.getHits())
        : processHitsOrderByIds(response.getHits(), ids);
    } catch(IndexMissingException e) {
      return Lists.newArrayList(); //ignoring
    }
  }

  protected List<T> processHitsOrderByIds(SearchHits hits, List<String> ids) {
    TreeMap<Integer, T> documents = new TreeMap<>();

    hits.forEach(hit -> {
      try {
        int position = ids.indexOf(hit.getId());
        if(position != -1) documents.put(position, processHit(hit));
      } catch(IOException e) {
        throw new RuntimeException(e);
      }
    });

    return documents.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
  }

  protected List<T> processHits(SearchHits hits) {
    List<T> documents = Lists.newArrayList();
    hits.forEach(hit -> {
      try {
        documents.add(processHit(hit));
      } catch(IOException e) {
        throw new RuntimeException(e);
      }
    });

    return documents;
  }

  protected QueryBuilder buildFilteredQuery(List<String> ids) {
    IdsQueryBuilder builder = QueryBuilders.idsQuery(getType());
    ids.forEach(builder::addIds);
    return builder;
  }

  protected FilterBuilder filterByStudy(String studyId) {
    return FilterBuilders.termFilter("studyIds", studyId);
  }

  protected List<String> getLocalizedFields(String... fieldNames) {
    List<String> fields = Lists.newArrayList();
    Stream.concat(micaConfigService.getConfig().getLocalesAsString().stream(), Stream.of(LanguageTag.UNDETERMINED))
      .forEach(locale -> {
        Arrays.stream(fieldNames).forEach(f -> {
          fields.add(f + "." + locale + ".analyzed");
        });
      });
    return fields;
  }
}
