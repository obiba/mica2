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

import com.google.common.base.Strings;

import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.web.model.MicaSearch;

import java.util.List;

import static org.obiba.mica.web.model.MicaSearch.*;

public final class QueryDtoHelper {

  private QueryDtoHelper() {}

  public static FilterQueryDto createTermFilter(String filterName, List values) {
    return FilterQueryDto.newBuilder().setField(filterName).setExtension(MicaSearch.TermsFilterQueryDto.terms,
        MicaSearch.TermsFilterQueryDto.newBuilder().addAllValues(values).build()).build();
  }

  public static QueryDto addTermFilters(QueryDto queryDto, List<FilterQueryDto> filters) {
    return QueryDto.newBuilder(queryDto).setFilteredQuery(FilteredQueryDto.newBuilder().setFilter(BoolFilterQueryDto
        .newBuilder(
            BoolFilterQueryDto.newBuilder(queryDto.getFilteredQuery().getFilter()).addAllMust(filters).build())))
        .build();
  }

  public static boolean hasQuery(QueryDto queryDto) {
    return queryDto != null && (queryDto.hasFilteredQuery() || queryDto.hasQuery());
  }

  public static QueryDto addShouldBoolFilters(QueryDto queryDto, List<FilterQueryDto> filters) {
    BoolFilterQueryDto.Builder boolFilter = BoolFilterQueryDto.newBuilder(queryDto.getFilteredQuery().getFilter());
    boolFilter.addAllShould(filters);

    return QueryDto.newBuilder(queryDto).setFilteredQuery(FilteredQueryDto.newBuilder().setFilter(boolFilter.build()))
        .build();
  }

  public static QueryDto createQueryDto(int from, int limit, String sort, String order, String queryString) {
    QueryDto.Builder builder = QueryDto.newBuilder().setFrom(from).setSize(limit);

    if(!Strings.isNullOrEmpty(queryString)) {
      builder.setQuery(queryString);
    }

    if(!Strings.isNullOrEmpty(sort)) {
      builder.setSort(QueryDto.SortDto.newBuilder() //
          .setField(sort) //
          .setOrder(order == null ? QueryDto.SortDto.Order.ASC : QueryDto.SortDto.Order.valueOf(order.toUpperCase()))
          .build());
    }

    return builder.build();
  }

}
