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
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.obiba.mica.web.model.MicaSearch;

import static org.obiba.mica.web.model.MicaSearch.BoolFilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FilteredQueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryDto;

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

    if(boolFilterDto.getTermsList().size() > 0) {
      boolFilterDto.getTermsList()
          .forEach(terms -> boolFilter.must(FilterBuilders.termsFilter(terms.getField(), terms.getValuesList())));
    }

    if(boolFilterDto.hasParentChildFilter()) {
      MicaSearch.ParentChildFilterDto parentChildFilterDto = boolFilterDto.getParentChildFilter();
      FilteredQueryBuilder dtoFilteredQuery = parseFilterQuery(parentChildFilterDto.getFilteredQuery());
      String type = parentChildFilterDto.getType();
      boolFilter.must(
          parentChildFilterDto.getRelationship() == MicaSearch.ParentChildFilterDto.Relationship.PARENT ? FilterBuilders
              .hasParentFilter(type, dtoFilteredQuery) : FilterBuilders.hasChildFilter(type, dtoFilteredQuery));
    }

    return boolFilter;
  }

}
