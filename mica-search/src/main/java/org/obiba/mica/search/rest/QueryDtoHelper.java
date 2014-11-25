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

    MicaSearch.FilteredQueryDto filteredQueryDto = queryDto.getFilteredQuery();

    if (filteredQueryDto.hasExtension(MicaSearch.LogicalFilterQueryDto.filter)) {
      return addLogicalTermFilters(queryDto, filters, type, filteredQueryDto);
    }

    return addBoolTermFilters(queryDto, filters, type, filteredQueryDto);
  }

  private static MicaSearch.QueryDto addBoolTermFilters(MicaSearch.QueryDto queryDto,
      List<MicaSearch.FieldFilterQueryDto> filters, BoolQueryType type, MicaSearch.FilteredQueryDto filteredQueryDto) {
    return MicaSearch.QueryDto.newBuilder(queryDto).setFilteredQuery(MicaSearch.FilteredQueryDto.newBuilder()
        .setExtension(MicaSearch.BoolFilterQueryDto.filter,
            createBoolQueryDto(filters, type, filteredQueryDto.getExtension(MicaSearch.BoolFilterQueryDto.filter))))
        .build();
  }

  /**
   * This is for simple cases when the input boolean query is not nested and has one operator
   * @param queryDto
   * @param filters
   * @param type
   * @param filteredQueryDto
   * @return
   */
  public static MicaSearch.QueryDto addLogicalTermFilters(MicaSearch.QueryDto queryDto,
      List<MicaSearch.FieldFilterQueryDto> filters, BoolQueryType type, MicaSearch.FilteredQueryDto filteredQueryDto) {
    MicaSearch.LogicalFilterQueryDto.Builder builder = MicaSearch.LogicalFilterQueryDto
        .newBuilder(filteredQueryDto.getExtension(MicaSearch.LogicalFilterQueryDto.filter));

    MicaSearch.FieldStatementDto.Operator logicalOp = type == BoolQueryType.MUST
        ? MicaSearch.FieldStatementDto.Operator._AND
        : MicaSearch.FieldStatementDto.Operator._OR;

    // make sure the last field statement has the input operator complying with LogicalFilterQueryDto rule
    if (builder.getFieldsCount() > 0) {
      int lastField = builder.getFieldsCount() - 1;
      MicaSearch.FieldStatementDto b = builder.getFields(lastField);
      builder.setFields(lastField, MicaSearch.FieldStatementDto.newBuilder(b).setOp(logicalOp));
    }

    filters.forEach(f -> builder.addFields(MicaSearch.FieldStatementDto.newBuilder().setOp(logicalOp).setField(f)));

    return MicaSearch.QueryDto.newBuilder(queryDto).setFilteredQuery(MicaSearch.FilteredQueryDto.newBuilder()
        .setExtension(MicaSearch.LogicalFilterQueryDto.filter, builder.build())).build();
  }

  public static MicaSearch.QueryDto createTermFiltersQuery(List<String> fields, List<String> values,
    BoolQueryType type) {
    MicaSearch.QueryDto.Builder builder = MicaSearch.QueryDto.newBuilder().setSize(DEFAULT_SIZE).setFrom(DEFAULT_FROM);
    if(values != null && values.size() > 0) {
      List<MicaSearch.FieldFilterQueryDto> filters = createTermFilters(fields, values);
      builder
        .setFilteredQuery(MicaSearch.FilteredQueryDto.newBuilder()
          .setExtension(MicaSearch.BoolFilterQueryDto.filter, createBoolQueryDto(filters, type, null)));
    }

    return builder.build();
  }

  public static MicaSearch.BoolFilterQueryDto createBoolQueryDto(List<MicaSearch.FieldFilterQueryDto> filters,
      BoolQueryType type, MicaSearch.BoolFilterQueryDto existingBool) {

    MicaSearch.BoolFilterQueryDto.Builder boolBuilder = existingBool == null ? MicaSearch.BoolFilterQueryDto
        .newBuilder() : MicaSearch.BoolFilterQueryDto.newBuilder(existingBool);

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

    filters.forEach(filter -> boolBuilder.addFilteredQuery(
        MicaSearch.FilteredQueryDto.newBuilder().setExtension(MicaSearch.FieldFilterQueryDto.filter, filter)));

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
    String locale, Stream<String> localizedQueryFields) {
    return createQueryDto(from, limit, sort, order, queryString, locale, localizedQueryFields, null);
  }

  public static MicaSearch.QueryDto createQueryDto(int from, int limit, String sort, String order, String queryString,
    String locale, Stream<String> localizedQueryFields, Stream<String> queryFields) {
    MicaSearch.QueryDto.Builder builder = MicaSearch.QueryDto.newBuilder().setFrom(from).setSize(limit);

    if(!Strings.isNullOrEmpty(queryString)) {
      addQueryStringDto(builder, queryString, locale, localizedQueryFields, queryFields);
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
    Stream<String> localizedQueryFields, Stream<String> queryFields) {
    if(queryDto != null && queryDto.hasQueryString()) {
      MicaSearch.QueryDto.QueryStringDto queryStringDto = queryDto.getQueryString();

      MicaSearch.QueryDto.QueryStringDto.Builder qsBuilder = MicaSearch.QueryDto.QueryStringDto
        .newBuilder(queryStringDto);
      addQueryStringDtoFields(qsBuilder, locale, localizedQueryFields, queryFields);
      MicaSearch.QueryDto.Builder builder = MicaSearch.QueryDto.newBuilder(queryDto).setQueryString(qsBuilder);
      return builder.build();
    }

    return queryDto;
  }

  public static void addQueryStringDto(MicaSearch.QueryDto.Builder builder, String queryString, String locale,
    Stream<String> localizedQueryFields, Stream<String> queryFields) {
    MicaSearch.QueryDto.QueryStringDto.Builder qsBuilder = MicaSearch.QueryDto.QueryStringDto.newBuilder()
      .setQuery(queryString);
    addQueryStringDtoFields(qsBuilder, locale, localizedQueryFields, queryFields);
    builder.setQueryString(qsBuilder);
  }

  public static void addQueryStringDtoFields(MicaSearch.QueryDto.QueryStringDto.Builder builder, String locale,
    Stream<String> localizedQueryFields, Stream<String> queryFields) {

    if(localizedQueryFields != null) {
      List<String> currentFields = builder.getFieldsList();
      String postfix = "." + (Strings.isNullOrEmpty(locale) ? "*" : locale) + ".analyzed";
      localizedQueryFields.filter((s) -> currentFields.indexOf(s + postfix) == -1)
        .forEach(field -> builder.addFields(field + postfix));
    }
    if(queryFields != null) {
      List<String> currentFields = builder.getFieldsList();
      String postfix = ".analyzed";
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

  public static MicaSearch.FilteredQueryDto createFilteredQuery(MicaSearch.LogicalFilterQueryDto logicalFilter) {
    return MicaSearch.FilteredQueryDto.newBuilder() //
      .setExtension(MicaSearch.LogicalFilterQueryDto.filter, logicalFilter).build();
  }

}
