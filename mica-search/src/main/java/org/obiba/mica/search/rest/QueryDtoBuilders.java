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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.obiba.mica.web.model.MicaSearch;

import static org.obiba.mica.web.model.MicaSearch.ExtendedQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FilteredQueryDto;
import static org.obiba.mica.web.model.MicaSearch.JoinQueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryDto;

public class QueryDtoBuilders {


  public static class ExtendedQueryDtoBuilder {
    private QueryDto.Builder builder;
    private ExtendedQueryDto.Builder extendedBuilder;

    private ExtendedQueryDtoBuilder() {
      builder = QueryDto.newBuilder();
      extendedBuilder = ExtendedQueryDto.newBuilder();
    }

    public static ExtendedQueryDtoBuilder newBuilder() {
      return new ExtendedQueryDtoBuilder();
    }

    public ExtendedQueryDtoBuilder query(QueryDto value) {
      builder = QueryDto.newBuilder(value);
      return this;
    }

    public ExtendedQueryDtoBuilder queryBuilder(QueryDto.Builder value) {
      builder = value;
      return this;
    }

    public ExtendedQueryDtoBuilder from(int value) {
      builder.setFrom(value);
      return this;
    }

    public ExtendedQueryDtoBuilder size(int value) {
      builder.setSize(value);
      return this;
    }

    public ExtendedQueryDtoBuilder studyFilter(List<String> fields, String id) {
      List<FilterQueryDto> termFilters = fields.stream() //
          .map(field -> QueryDtoHelper.createTermFilter(field, Arrays.asList(id))) //
          .collect(Collectors.toList());
      builder.setFilteredQuery(FilteredQueryDto.newBuilder()
          .setFilter(MicaSearch.BoolFilterQueryDto.newBuilder().addAllShould(termFilters)));
      return this;
    }

    public ExtendedQueryDtoBuilder filtered(FilteredQueryDto.Builder value) {
      builder.setFilteredQuery(value);
      return this;
    }

    public ExtendedQueryDtoBuilder sort(String field, String order) {
      extendedBuilder.setSort(
          ExtendedQueryDto.SortDto.newBuilder().setField(field).setOrder(ExtendedQueryDto.SortDto.Order.valueOf(order)));
      return this;
    }

    public ExtendedQueryDtoBuilder queryString(String value) {
      extendedBuilder.setQuery(value);
      return this;
    }

    public QueryDto build() {
      if (extendedBuilder.isInitialized()) {
        builder.setExtension(ExtendedQueryDto.extended, extendedBuilder.build());
      }

      return builder.build();
    }
  }

  public static class StudyJoinQueryDtoBuilder {
    private JoinQueryDto.Builder builder;

    private StudyJoinQueryDtoBuilder() {
      builder = JoinQueryDto.newBuilder();
    }

    public static StudyJoinQueryDtoBuilder newBuilder() {
      return new StudyJoinQueryDtoBuilder();
    }

    public StudyJoinQueryDtoBuilder studyQuery(QueryDto value) {
      builder.setStudyQueryDto(value);
      return this;
    }

    public JoinQueryDto build() {
      return builder.build();
    }

  }
}
