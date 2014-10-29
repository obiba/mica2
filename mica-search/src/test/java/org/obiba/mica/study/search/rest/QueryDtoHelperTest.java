/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search.rest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.mica.search.rest.QueryDtoHelper;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.obiba.mica.search.rest.QueryDtoHelper.addTermFilters;
import static org.obiba.mica.web.model.MicaSearch.FilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryDto.QueryStringDto;
import static org.obiba.mica.web.model.MicaSearch.TermsFilterQueryDto;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { QueryDtoHelperTest.Config.class })
public class QueryDtoHelperTest {

  @Test
  public void test_create_term_filter() {
    FilterQueryDto filteredQuery = QueryDtoHelper.createTermFilter("studyId", Arrays.asList("100", "200", "300"));
    assertThat(filteredQuery.getField()).isEqualTo("studyId");
    TermsFilterQueryDto terms = filteredQuery.getExtension(TermsFilterQueryDto.terms);
    assertThat(terms).isNotNull();
    assertThat(terms.getValuesCount()).isEqualTo(3);
    assertThat(terms.getValues(1)).isEqualTo("200");
  }

  @Test
  public void test_create_term_filters() {
    List<FilterQueryDto> filterQueries = QueryDtoHelper
        .createTermFilters(Arrays.asList("id1", "id2"), Arrays.asList("100", "200", "300"));

    assertThat(filterQueries.size()).isEqualTo(2);
    assertThat(filterQueries.get(0).getField()).isEqualTo("id1");
    TermsFilterQueryDto terms = filterQueries.get(1).getExtension(TermsFilterQueryDto.terms);
    assertThat(terms).isNotNull();
    assertThat(terms.getValuesCount()).isEqualTo(3);
    assertThat(terms.getValues(1)).isEqualTo("200");
    System.out.println(filterQueries);
  }

  @Test
  public void test_add_term_filters() {
    List<FilterQueryDto> filterQueries = QueryDtoHelper
        .createTermFilters(Arrays.asList("id1", "id2"), Arrays.asList("100", "200", "300"));
    QueryDto queryDto = addTermFilters(createDummyQueryDto(QueryDtoHelper.BoolQueryType.SHOULD), filterQueries,
        QueryDtoHelper.BoolQueryType.SHOULD);
    assertThat(queryDto.getFilteredQuery()).isNotNull();
    assertThat(queryDto.getFilteredQuery().getFilter().getShouldCount()).isEqualTo(4);
    assertThat(queryDto.getFilteredQuery().getFilter().getShould(2).getField()).isEqualTo("id1");
    TermsFilterQueryDto terms = queryDto.getFilteredQuery().getFilter().getShould(3)
        .getExtension(TermsFilterQueryDto.terms);
    assertThat(terms.getValuesCount()).isEqualTo(3);
    assertThat(terms.getValues(1)).isEqualTo("200");
  }

  @Test
  public void test_create_term_filters_query() {
    QueryDto queryDto = QueryDtoHelper
        .createTermFiltersQuery(Arrays.asList("id1", "id2"), Arrays.asList("100", "200", "300"),
            QueryDtoHelper.BoolQueryType.MUST);

    assertThat(queryDto.getFilteredQuery()).isNotNull();
    assertThat(queryDto.getFilteredQuery().getFilter().getMustCount()).isEqualTo(2);
    TermsFilterQueryDto terms = queryDto.getFilteredQuery().getFilter().getMust(1)
        .getExtension(TermsFilterQueryDto.terms);
    assertThat(terms.getValuesCount()).isEqualTo(3);
    assertThat(terms.getValues(1)).isEqualTo("200");
  }

  @Test
  public void test() {
    List<FilterQueryDto> filterQueries = QueryDtoHelper
        .createTermFilters(Arrays.asList("id1", "id2"), Arrays.asList("100", "200", "300"));
    QueryDto queryDto = QueryDtoHelper
        .addTermFilters(createDummyQueryDto(QueryDtoHelper.BoolQueryType.SHOULD), filterQueries,
            QueryDtoHelper.BoolQueryType.SHOULD);

    System.out.println(queryDto);
    queryDto = QueryDtoHelper.addTermFilters(createDummyQueryDto(QueryDtoHelper.BoolQueryType.SHOULD), filterQueries, QueryDtoHelper.BoolQueryType.SHOULD);
    System.out.println(queryDto);
  }

  private QueryDto createDummyQueryDto(QueryDtoHelper.BoolQueryType boolType) {
    FilterQueryDto termsDto1 = FilterQueryDto.newBuilder().setField("access").setExtension(
        TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("data", "bio-samples")).build()).build();

    FilterQueryDto termsDto2 = FilterQueryDto.newBuilder().setField("start").setExtension(
        TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("2002")).build()).build();

    MicaSearch.BoolFilterQueryDto.Builder boolDtoBuilder = MicaSearch.BoolFilterQueryDto.newBuilder();
    switch(boolType) {
      case MUST:
        boolDtoBuilder.addMust(termsDto1).addMust(termsDto2);
        break;
      case SHOULD:
        boolDtoBuilder.addShould(termsDto1).addShould(termsDto2);
        break;
    }

    return QueryDto.newBuilder()
        .setFilteredQuery(MicaSearch.FilteredQueryDto.newBuilder().setFilter(boolDtoBuilder.build()).build()).setFrom(0)
        .setSize(10).build();
  }

  @Test
  public void test_query_string_no_existing_fields() {
    QueryStringDto queryStringDto = QueryStringDto.newBuilder().setQuery("toto").build();
    QueryStringDto.Builder builder = QueryStringDto.newBuilder(queryStringDto);
    QueryDtoHelper.addQueryStringDtoFields(builder, "de", Stream.of("field1", "field2"));
    assertThat(builder.getFieldsCount()).isEqualTo(2);
    assertThat(builder.getFields(0)).isEqualTo("field1.de.analyzed");
  }

  @Test
  public void test_query_string_duplicated_fields() {
    QueryStringDto queryStringDto = QueryStringDto.newBuilder().setQuery("toto")
        .addAllFields(Arrays.asList("field1.de.analyzed", "field2.de.analyzed")).build();

    QueryStringDto.Builder builder = QueryStringDto.newBuilder(queryStringDto);
    QueryDtoHelper.addQueryStringDtoFields(builder, "de", Stream.of("field2", "field3"));
    assertThat(builder.getFieldsCount()).isEqualTo(3);
    assertThat(builder.getFields(0)).isEqualTo("field1.de.analyzed");
    assertThat(builder.getFields(1)).isEqualTo("field2.de.analyzed");
    assertThat(builder.getFields(2)).isEqualTo("field3.de.analyzed");
  }

  @Test
  public void test_query_with_query_string() {
    QueryDto queryDto = QueryDtoHelper.createQueryDto(0, 20, null, null, "tata", "fr", Stream.of("field1", "field2"));
    assertThat(queryDto.getFrom()).isEqualTo(0);
    assertThat(queryDto.getSize()).isEqualTo(20);
    assertThat(queryDto.hasQueryString()).isTrue();
    QueryStringDto qsDto = queryDto.getQueryString();
    assertThat(qsDto.getQuery()).isEqualTo("tata");
    assertThat(qsDto.getFieldsCount()).isEqualTo(2);
    assertThat(qsDto.getFields(0)).isEqualTo("field1.fr.analyzed");
    assertThat(qsDto.getFields(1)).isEqualTo("field2.fr.analyzed");
  }

  @Test
  public void test_ensure_query_string_fields() {
    QueryDto queryDto = QueryDto.newBuilder().setFrom(0).setSize(20)
        .setQueryString(QueryStringDto.newBuilder().setQuery("zombie")).build();
    assertThat(queryDto.getFrom()).isEqualTo(0);
    assertThat(queryDto.getSize()).isEqualTo(20);
    assertThat(queryDto.hasQueryString()).isTrue();
    QueryStringDto qsDto = queryDto.getQueryString();
    assertThat(qsDto.getQuery()).isEqualTo("zombie");
    assertThat(qsDto.getFieldsCount()).isEqualTo(0);

    queryDto = QueryDtoHelper.ensureQueryStringDtoFields(queryDto, "nl", Stream.of("field1", "field2"));
    assertThat(queryDto.getFrom()).isEqualTo(0);
    assertThat(queryDto.getSize()).isEqualTo(20);
    assertThat(queryDto.hasQueryString()).isTrue();
    qsDto = queryDto.getQueryString();
    assertThat(qsDto.getQuery()).isEqualTo("zombie");
    assertThat(qsDto.getFieldsCount()).isEqualTo(2);
    assertThat(qsDto.getFields(0)).isEqualTo("field1.nl.analyzed");
    assertThat(qsDto.getFields(1)).isEqualTo("field2.nl.analyzed");
  }

  @Configuration
  static class Config {}

}

