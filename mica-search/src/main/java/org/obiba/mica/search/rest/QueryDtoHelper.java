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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.obiba.mica.web.model.MicaSearch;

import com.google.common.base.Strings;

public final class QueryDtoHelper {

  public static final int DEFAULT_FROM = 0;

  public static final int DEFAULT_SIZE = 10;

  public enum BoolQueryType {
    MUST,
    SHOULD,
    NOT
  }

  private QueryDtoHelper() {}

  public static MicaSearch.FieldFilterQueryDto createTermFilter(String filterName, List values) {
    return MicaSearch.FieldFilterQueryDto.newBuilder().setField(filterName)
      .setExtension(MicaSearch.TermsFilterQueryDto.terms,
        MicaSearch.TermsFilterQueryDto.newBuilder().addAllValues(values).build()).build();
  }

  public static MicaSearch.QueryDto addTermFilters(MicaSearch.QueryDto queryDto,
    List<MicaSearch.FieldFilterQueryDto> filters, BoolQueryType type) {

    return MicaSearch.QueryDto.newBuilder(queryDto).setFilteredQuery(MicaSearch.FilteredQueryDto.newBuilder()
      .setExtension(MicaSearch.BoolFilterQueryDto.filter, createBoolQueryDto(filters, type))).build();
  }

  public static MicaSearch.QueryDto createTermFiltersQuery(List<String> fields, List<String> values,
    BoolQueryType type) {
    MicaSearch.QueryDto.Builder builder = MicaSearch.QueryDto.newBuilder().setSize(DEFAULT_SIZE).setFrom(DEFAULT_FROM);
    if(values != null && values.size() > 0) {
      List<MicaSearch.FieldFilterQueryDto> filters = createTermFilters(fields, values);
      builder
        .setFilteredQuery(MicaSearch.FilteredQueryDto.newBuilder()
          .setExtension(MicaSearch.BoolFilterQueryDto.filter, createBoolQueryDto(filters, type)));
    }

    return builder.build();
  }

  public static MicaSearch.BoolFilterQueryDto createBoolQueryDto(List<MicaSearch.FieldFilterQueryDto> filters, BoolQueryType type) {
    MicaSearch.BoolFilterQueryDto.Builder boolBuilder = MicaSearch.BoolFilterQueryDto.newBuilder();

    switch(type) {
      case MUST:
        boolBuilder.setOp(MicaSearch.BoolFilterQueryDto.Operator.MUST);
        break;
      case SHOULD:
        boolBuilder.setOp(MicaSearch.BoolFilterQueryDto.Operator.SHOULD);
        break;
      case NOT:
        boolBuilder.setOp(MicaSearch.BoolFilterQueryDto.Operator.MUST_NOT);
        break;
    }

    filters.forEach(filter -> boolBuilder.addFilteredQuery(MicaSearch.FilteredQueryDto.newBuilder().setExtension(MicaSearch.FieldFilterQueryDto.filter, filter)));

    return boolBuilder.build();
  }

  public static List<MicaSearch.FieldFilterQueryDto> createTermFilters(List<String> fields, List<String> studyIds) {
    return fields //
      .stream() //
      .map(field -> createTermFilter(field, studyIds)) //
      .collect(Collectors.toList());
  }

  public static boolean hasQuery(MicaSearch.QueryDto queryDto) {
    return queryDto != null && (queryDto.hasFilteredQuery() || queryDto.hasQueryString());
  }

  public static MicaSearch.QueryDto createQueryDto(int from, int limit, String sort, String order, String queryString,
    String locale, Stream<String> queryFields) {
    MicaSearch.QueryDto.Builder builder = MicaSearch.QueryDto.newBuilder().setFrom(from).setSize(limit);

    if(!Strings.isNullOrEmpty(queryString)) {
      addQueryStringDto(builder, queryString, locale, queryFields);
    }

    if(!Strings.isNullOrEmpty(sort)) {
      builder.setSort(MicaSearch.QueryDto.SortDto.newBuilder() //
        .setField(sort) //
        .setOrder(order == null
          ? MicaSearch.QueryDto.SortDto.Order.ASC
          : MicaSearch.QueryDto.SortDto.Order.valueOf(order.toUpperCase())).build());
    }

    return builder.build();
  }

  public static MicaSearch.QueryDto ensureQueryStringDtoFields(MicaSearch.QueryDto queryDto, String locale,
    Stream<String> queryFields) {
    if(queryDto != null && queryDto.hasQueryString()) {
      MicaSearch.QueryDto.QueryStringDto queryStringDto = queryDto.getQueryString();

      MicaSearch.QueryDto.QueryStringDto.Builder qsBuilder = MicaSearch.QueryDto.QueryStringDto
        .newBuilder(queryStringDto);
      addQueryStringDtoFields(qsBuilder, locale, queryFields);
      MicaSearch.QueryDto.Builder builder = MicaSearch.QueryDto.newBuilder(queryDto).setQueryString(qsBuilder);
      return builder.build();
    }

    return queryDto;
  }

  public static void addQueryStringDto(MicaSearch.QueryDto.Builder builder, String queryString, String locale,
    Stream<String> queryFields) {
    MicaSearch.QueryDto.QueryStringDto.Builder qsBuilder = MicaSearch.QueryDto.QueryStringDto.newBuilder()
      .setQuery(queryString);
    addQueryStringDtoFields(qsBuilder, locale, queryFields);
    builder.setQueryString(qsBuilder);
  }

  public static void addQueryStringDtoFields(MicaSearch.QueryDto.QueryStringDto.Builder builder, String locale,
    Stream<String> queryFields) {

    if(queryFields != null) {
      List<String> currentFields = builder.getFieldsList();
      String postfix = "." + (Strings.isNullOrEmpty(locale) ? "*" : locale) + ".analyzed";
      queryFields.filter((s) -> currentFields.indexOf(s + postfix) == -1)
        .forEach(field -> builder.addFields(field + postfix));
    }
  }

  public static MicaSearch.FilteredQueryDto createFilteredQuery(MicaSearch.FieldFilterQueryDto fieldFilter) {
    return MicaSearch.FilteredQueryDto.newBuilder() //
    .setExtension(MicaSearch.FieldFilterQueryDto.filter, fieldFilter).build();
  }

  public static MicaSearch.FilteredQueryDto createFilteredQuery(MicaSearch.BoolFilterQueryDto boolFilter) {
    return MicaSearch.FilteredQueryDto.newBuilder() //
      .setExtension(MicaSearch.BoolFilterQueryDto.filter, boolFilter).build();
  }

}
