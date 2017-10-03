/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries.protobuf;

import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.obiba.mica.search.TaxonomyFilterParser;
import org.obiba.mica.search.queries.QueryWrapper;
import org.obiba.mica.web.model.MicaSearch;

import com.google.common.collect.Lists;

/**
 *
 */
public class QueryDtoWrapper implements QueryWrapper {

  private final MicaSearch.QueryDto queryDto;

  private QueryBuilder queryBuilder;

  private final List<SortBuilder> sortBuilders;

  private List<String> aggregations;

  public QueryDtoWrapper(MicaSearch.QueryDto queryDto) {
    this.queryDto = queryDto;
    QueryDtoParser queryDtoParser = QueryDtoParser.newParser();
    queryBuilder = queryDtoParser.parse(queryDto);
    sortBuilders = queryDtoParser.parseSort(queryDto);
  }

  @Override
  public boolean isValid() {
    return queryBuilder != null;
  }

  @Override
  public QueryBuilder getQueryBuilder() {
    return queryBuilder;
  }

  @Override
  public void setQueryBuilder(QueryBuilder queryBuilder) {
    this.queryBuilder = queryBuilder;
  }

  @Override
  public List<SortBuilder> getSortBuilders() {
    return sortBuilders;
  }

  @Override
  public int getFrom() {
    return queryDto != null ? queryDto.getFrom() : DEFAULT_FROM;
  }

  @Override
  public int getSize() {
    return queryDto != null ? queryDto.getSize() : DEFAULT_SIZE;
  }

  @Override
  public List<String> getSourceFields() {
    return Lists.newArrayList();
  }

  @Override
  public List<String> getAggregationBuckets() {
    return queryDto != null ? queryDto.getAggsByList() : Lists.newArrayList();
  }

  @Override
  public List<String> getAggregations() {
    if (aggregations == null) aggregations = Lists.newArrayList();
    return aggregations;
  }

  @Override
  public Map<String, Map<String, List<String>>> getTaxonomyTermsMap() {
    TaxonomyFilterParser parser = new TaxonomyFilterParser(queryDto);
    return parser.getTermsMap();
  }
}
