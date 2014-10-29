/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.rest;

import org.elasticsearch.index.query.*;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.obiba.mica.web.model.MicaSearch;

import static org.obiba.mica.web.model.MicaSearch.*;

public class QueryDtoParser {

  private QueryDtoParser() {}

  public static QueryDtoParser newParser() {
    return new QueryDtoParser();
  }

  public BaseQueryBuilder parse(QueryDto queryDto) {
    BaseQueryBuilder query = null;

    if(queryDto.hasQueryString()) {
      QueryStringQueryBuilder queryStringBuilder = QueryBuilders.queryString(queryDto.getQueryString().getQuery());
      queryDto.getQueryString().getFieldsList().forEach(queryStringBuilder::field);
      query = queryStringBuilder;
    }

    return parseFilterQuery(query == null ? QueryBuilders.matchAllQuery() : query, queryDto.getFilteredQuery());
  }

  public SortBuilder parseSort(QueryDto queryDto) {

    if(queryDto.hasSort()) {
      QueryDto.SortDto sortDto = queryDto.getSort();
      return SortBuilders.fieldSort(sortDto.getField()).order(SortOrder.valueOf(sortDto.getOrder().name()));
    }

    return null;
  }

  private BaseQueryBuilder parseFilterQuery(BaseQueryBuilder query, FilteredQueryDto filteredQueryDto) {
    if(filteredQueryDto.hasFilter()) {
      BoolFilterQueryDto boolFilterDto = filteredQueryDto.getFilter();
      BoolFilterBuilder boolFilter = parseBoolFilter(boolFilterDto);
      return QueryBuilders.filteredQuery(query, boolFilter);
    }
    return query;
  }

  private BoolFilterBuilder parseBoolFilter(BoolFilterQueryDto boolFilterDto) {
    BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();

    if(boolFilterDto.getMustCount() > 0) {
      boolFilterDto.getMustList().forEach(filter -> boolFilter.must(parseFilter(filter)));
    }

    if(boolFilterDto.getMustNotCount() > 0) {
      boolFilterDto.getMustNotList().forEach(filter -> boolFilter.mustNot(parseFilter(filter)));
    }

    if(boolFilterDto.getShouldCount() > 0) {
      boolFilterDto.getShouldList().forEach(filter -> boolFilter.should(parseFilter(filter)));
    }

    if(boolFilterDto.hasParentChildFilter()) {
      ParentChildFilterDto parentChildFilterDto = boolFilterDto.getParentChildFilter();
      BaseQueryBuilder dtoFilteredQuery = parseFilterQuery(QueryBuilders.matchAllQuery(),
          parentChildFilterDto.getFilteredQuery());
      String type = parentChildFilterDto.getType();
      boolFilter.must(
          parentChildFilterDto.getRelationship() == ParentChildFilterDto.Relationship.PARENT ? FilterBuilders
              .hasParentFilter(type, dtoFilteredQuery) : FilterBuilders.hasChildFilter(type, dtoFilteredQuery));
    }

    return boolFilter;
  }

  private FilterBuilder parseFilter(FilterQueryDto filter) {
    String field = filter.getField();
    if(filter.hasExtension(MicaSearch.TermsFilterQueryDto.terms)) {
      return FilterBuilders
          .termsFilter(field, filter.getExtension(MicaSearch.TermsFilterQueryDto.terms).getValuesList());
    } else if(filter.hasExtension(RangeFilterQueryDto.range)) {
      return parseRangeFilter(field, filter.getExtension(RangeFilterQueryDto.range));
    }

    throw new IllegalArgumentException("Invalid filter extension");
  }

  private FilterBuilder parseRangeFilter(String field, RangeFilterQueryDto rangeFilterDto) {
    RangeFilterBuilder filterBuilder = FilterBuilders.rangeFilter(field);
    if(rangeFilterDto.hasFrom()) parseRangeFilterCondition(filterBuilder, rangeFilterDto.getFrom());
    if(rangeFilterDto.hasTo()) parseRangeFilterCondition(filterBuilder, rangeFilterDto.getTo());
    return filterBuilder;
  }

  private void parseRangeFilterCondition(RangeFilterBuilder filterBuilder, RangeConditionDto condition) {
    String value = condition.getValue();
    switch(condition.getOp()) {
      case LT:
        filterBuilder.lt(value);
        break;
      case LTE:
        filterBuilder.lte(value);
        break;
      case GT:
        filterBuilder.gt(value);
        break;
      case GTE:
        filterBuilder.gte(value);
        break;
    }
  }

}
