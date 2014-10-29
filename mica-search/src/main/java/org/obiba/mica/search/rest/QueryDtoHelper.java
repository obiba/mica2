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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.obiba.mica.web.model.MicaSearch;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import static org.obiba.mica.web.model.MicaSearch.BoolFilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FilteredQueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryDto.QueryStringDto;

public final class QueryDtoHelper {

  public static final int DEFAULT_FROM = 0;

  public static final int DEFAULT_SIZE = 10;

  public enum BoolQueryType {
    MUST,
    SHOULD,
    NOT
  }

  private QueryDtoHelper() {}

  public static FilterQueryDto createTermFilter(String filterName, List values) {
    return FilterQueryDto.newBuilder().setField(filterName).setExtension(MicaSearch.TermsFilterQueryDto.terms,
        MicaSearch.TermsFilterQueryDto.newBuilder().addAllValues(values).build()).build();
  }

  public static QueryDto addTermFilters(QueryDto queryDto, List<FilterQueryDto> filters, BoolQueryType type) {
    return QueryDto.newBuilder(queryDto).setFilteredQuery(FilteredQueryDto.newBuilder()
        .setFilter(createBoolQueryDto(queryDto.getFilteredQuery().getFilter(), filters, type))).build();
  }

  public static QueryDto createTermFiltersQuery(List<String> fields, List<String> values, BoolQueryType type) {
    QueryDto.Builder builder = QueryDto.newBuilder().setSize(DEFAULT_SIZE).setFrom(DEFAULT_FROM);
    if(values != null && values.size() > 0) {
      List<FilterQueryDto> filters = createTermFilters(fields, values);
      builder.setFilteredQuery(
          MicaSearch.FilteredQueryDto.newBuilder().setFilter(createBoolQueryDto(null, filters, type)));
    }

    return builder.build();
  }

  public static BoolFilterQueryDto createBoolQueryDto(BoolFilterQueryDto template, List<FilterQueryDto> filters,
      BoolQueryType type) {
    MicaSearch.BoolFilterQueryDto.Builder boolBuilder = template == null
        ? MicaSearch.BoolFilterQueryDto.newBuilder()
        : MicaSearch.BoolFilterQueryDto.newBuilder(template);

    switch(type) {
      case MUST:
        boolBuilder.addAllMust(filters);
        break;
      case SHOULD:
        boolBuilder.addAllShould(filters);
        break;
      case NOT:
        boolBuilder.addAllMustNot(filters);
        break;
    }

    return boolBuilder.build();
  }

  public static List<MicaSearch.FilterQueryDto> createTermFilters(List<String> fields, List<String> studyIds) {
    return fields //
        .stream() //
        .map(field -> createTermFilter(field, studyIds)) //
        .collect(Collectors.toList());
  }

  public static List<String> getTermsFilterValues(QueryDto queryDto, String field,
      Function<QueryDto, List<FilterQueryDto>> filters) {

    List<String> values = filters.apply(queryDto).stream().filter(q -> q.getField().equals(field))
        .map(q -> q.getExtension(MicaSearch.TermsFilterQueryDto.terms).getValuesList()).flatMap((t) -> t.stream())
        .collect(Collectors.toList());

    return values;
  }

  public static Function<QueryDto, List<FilterQueryDto>> getTermsMustFilters() {
    return queryDto -> {
      if(queryDto.hasFilteredQuery() && queryDto.getFilteredQuery().hasFilter()) {
        return queryDto.getFilteredQuery().getFilter().getMustList();
      }

      return Lists.newArrayList();
    };
  }

  public static Function<QueryDto, List<FilterQueryDto>> getTermsShouldFilters() {
    return queryDto -> {
      if(queryDto.hasFilteredQuery() && queryDto.getFilteredQuery().hasFilter()) {
        return queryDto.getFilteredQuery().getFilter().getShouldList();
      }

      return Lists.newArrayList();
    };
  }

  public static boolean hasQuery(QueryDto queryDto) {
    return queryDto != null && (queryDto.hasFilteredQuery() || queryDto.hasQueryString());
  }

  public static QueryDto createQueryDto(int from, int limit, String sort, String order, String queryString,
      String locale, Stream<String> queryFields) {
    QueryDto.Builder builder = QueryDto.newBuilder().setFrom(from).setSize(limit);

    if(!Strings.isNullOrEmpty(queryString)) {
      addQueryStringDto(builder, queryString, locale, queryFields);
    }

    if(!Strings.isNullOrEmpty(sort)) {
      builder.setSort(QueryDto.SortDto.newBuilder() //
          .setField(sort) //
          .setOrder(order == null ? QueryDto.SortDto.Order.ASC : QueryDto.SortDto.Order.valueOf(order.toUpperCase()))
          .build());
    }

    return builder.build();
  }

  public static QueryDto ensureQueryStringDtoFields(QueryDto queryDto, String locale, Stream<String> queryFields) {
    if(queryDto != null && queryDto.hasQueryString()) {
      QueryStringDto queryStringDto = queryDto.getQueryString();

      QueryStringDto.Builder qsBuilder = QueryStringDto.newBuilder(queryStringDto);
      addQueryStringDtoFields(qsBuilder, locale, queryFields);
      QueryDto.Builder builder = QueryDto.newBuilder(queryDto).setQueryString(qsBuilder);
      return builder.build();
    }

    return queryDto;
  }

  public static void addQueryStringDto(QueryDto.Builder builder, String queryString, String locale,
      Stream<String> queryFields) {
    QueryStringDto.Builder qsBuilder = QueryStringDto.newBuilder().setQuery(queryString);
    addQueryStringDtoFields(qsBuilder, locale, queryFields);
    builder.setQueryString(qsBuilder);
  }

  public static void addQueryStringDtoFields(QueryStringDto.Builder builder, String locale,
      Stream<String> queryFields) {

    if(queryFields != null) {
      List<String> currentFields = builder.getFieldsList();
      String postfix = "." + (Strings.isNullOrEmpty(locale) ? "*" : locale) + ".analyzed";
      queryFields.filter((s) -> currentFields.indexOf(s + postfix) == -1)
          .forEach(field -> builder.addFields(field + postfix));
    }
  }

}
