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

import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.obiba.mica.web.model.MicaSearch;

import static org.obiba.mica.web.model.MicaSearch.BoolFilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FilteredQueryDto;
import static org.obiba.mica.web.model.MicaSearch.ParentChildFilterDto;
import static org.obiba.mica.web.model.MicaSearch.QueryDto;
import static org.obiba.mica.web.model.MicaSearch.RangeConditionDto;
import static org.obiba.mica.web.model.MicaSearch.RangeFilterQueryDto;

public class QueryDtoParser {

  private QueryDtoParser() {}

  public static QueryDtoParser newParser() {
    return new QueryDtoParser();
  }

  public FilteredQueryBuilder parse(QueryDto queryDto) {
    return parseFilterQuery(queryDto.getFilteredQuery());
  }

  private FilteredQueryBuilder parseFilterQuery(FilteredQueryDto filteredQueryDto) {
    BoolFilterQueryDto boolFilterDto = filteredQueryDto.getFilter();
    BoolFilterBuilder boolFilter = parseBoolFilter(boolFilterDto);
    return QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), boolFilter);
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
      FilteredQueryBuilder dtoFilteredQuery = parseFilterQuery(parentChildFilterDto.getFilteredQuery());
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
    } else if (filter.hasExtension(RangeFilterQueryDto.range)) {
      return parseRangeFilter(field, filter.getExtension(RangeFilterQueryDto.range));
    }

    throw new IllegalArgumentException("Invalid filter extension");
  }

  private FilterBuilder parseRangeFilter(String field, RangeFilterQueryDto rangeFilterDto) {
    RangeFilterBuilder filterBuilder = FilterBuilders.rangeFilter(field);
    if (rangeFilterDto.hasFrom()) parseRangeFilterCondition(filterBuilder, rangeFilterDto.getFrom());
    if (rangeFilterDto.hasTo()) parseRangeFilterCondition(filterBuilder, rangeFilterDto.getTo());
    return filterBuilder;
  }

  private void parseRangeFilterCondition(RangeFilterBuilder filterBuilder, RangeConditionDto condition) {
    String value = condition.getValue();
    switch (condition.getOp()) {
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
