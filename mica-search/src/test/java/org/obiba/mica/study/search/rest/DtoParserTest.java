/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.mica.study.search.rest;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.mica.search.queries.protobuf.QueryDtoHelper;
import org.obiba.mica.search.queries.protobuf.QueryDtoParser;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.obiba.mica.web.model.MicaSearch.BoolFilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FieldFilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FieldStatementDto;
import static org.obiba.mica.web.model.MicaSearch.FilteredQueryDto;
import static org.obiba.mica.web.model.MicaSearch.LogicalFilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryDto;
import static org.obiba.mica.web.model.MicaSearch.RangeConditionDto;
import static org.obiba.mica.web.model.MicaSearch.RangeFilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.TermsFilterQueryDto;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DtoParserTest.Config.class })
public class DtoParserTest {

  @Test
  public void test_query_dto_parser() throws IOException {
    FieldFilterQueryDto termsDto1 = FieldFilterQueryDto.newBuilder().setField("access")
      .setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("data", "bio-samples")).build()).build();

    FieldFilterQueryDto termsDto2 = FieldFilterQueryDto.newBuilder().setField("start")
      .setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("2002")).build()).build();

    BoolFilterQueryDto boolDto = BoolFilterQueryDto.newBuilder() //
      .setOp(BoolFilterQueryDto.Operator.MUST) //
      .addFilteredQuery(QueryDtoHelper.createFilteredQuery(termsDto1))//
      .addFilteredQuery(QueryDtoHelper.createFilteredQuery(termsDto2)).build();

    FilteredQueryDto filteredDto = QueryDtoHelper.createFilteredQuery(boolDto);
    QueryDto queryDto = QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10).build();

    QueryDtoParser parser = QueryDtoParser.newParser();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode expected = mapper.readTree(
      "{\"bool\":{\"must\":[{\"match_all\":{}},{\"bool\":{\"must\":[{\"terms\":{\"access\":[\"data\",\"bio-samples\"]}},{\"terms\":{\"start\":[\"2002\"]}}]}}]}}");
    JsonNode actual = mapper.readTree(parser.parse(queryDto).toString());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void test_query_dto_parser_terms_range_filter() throws IOException {
    FieldFilterQueryDto termsDto = FieldFilterQueryDto.newBuilder()
      .setField("Study.populations.dataCollectionEvents.id").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("53f4b8ab6cf07b0996deb4f7")).build()).build();

    FieldFilterQueryDto rangeDto = FieldFilterQueryDto.newBuilder()
      .setField("Study.populations.dataCollectionEvents.end").setExtension(RangeFilterQueryDto.range,
        RangeFilterQueryDto.newBuilder()
          .setFrom(RangeConditionDto.newBuilder().setOp(RangeConditionDto.Operator.GTE).setValue("2002"))
          .setTo(RangeConditionDto.newBuilder().setOp(RangeConditionDto.Operator.LTE).setValue("2012")).build())
      .build();

    BoolFilterQueryDto boolDto = BoolFilterQueryDto.newBuilder() //
      .setOp(BoolFilterQueryDto.Operator.MUST) //
      .addFilteredQuery(QueryDtoHelper.createFilteredQuery(termsDto)) //
      .addFilteredQuery(QueryDtoHelper.createFilteredQuery(rangeDto)).build();

    FilteredQueryDto filteredDto = QueryDtoHelper.createFilteredQuery(boolDto);
    QueryDto queryDto = QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10).build();

    QueryDtoParser parser = QueryDtoParser.newParser();
    System.out.println(parser.parse(queryDto).toString());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode expected = mapper.readTree(
      "{\"bool\":{\"must\":[{\"match_all\":{}},{\"bool\":{\"must\":[{\"terms\":{\"Study.populations.dataCollectionEvents.id\":[\"53f4b8ab6cf07b0996deb4f7\"]}},{\"range\":{\"Study.populations.dataCollectionEvents.end\":{\"from\":\"2002\",\"to\":\"2012\",\"include_lower\":true,\"include_upper\":true}}}]}}]}}");
    JsonNode actual = mapper.readTree(parser.parse(queryDto).toString());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void test_query_dto_parser_bool_must_must_not_should() throws IOException {
    FieldFilterQueryDto termsDto = FieldFilterQueryDto.newBuilder()
      .setField("Study.populations.dataCollectionEvents.id").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("53f4b8ab6cf07b0996deb4f7")).build()).build();

    FieldFilterQueryDto rangeDto = FieldFilterQueryDto.newBuilder()
      .setField("Study.populations.dataCollectionEvents.end").setExtension(RangeFilterQueryDto.range,
        RangeFilterQueryDto.newBuilder()
          .setFrom(RangeConditionDto.newBuilder().setOp(RangeConditionDto.Operator.GTE).setValue("2002"))
          .setTo(RangeConditionDto.newBuilder().setOp(RangeConditionDto.Operator.LTE).setValue("2012")).build())
      .build();

    FieldFilterQueryDto badTermsDto = FieldFilterQueryDto.newBuilder()
      .setField("Study.populations.dataCollectionEvents.id").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("aaaaaa")).build()).build();

    BoolFilterQueryDto boolDto = BoolFilterQueryDto.newBuilder() //
      .setOp(BoolFilterQueryDto.Operator.MUST) //
      .addFilteredQuery(QueryDtoHelper.createFilteredQuery(termsDto)) //
      .addFilteredQuery(QueryDtoHelper.createFilteredQuery(BoolFilterQueryDto.newBuilder() //
        .setOp(BoolFilterQueryDto.Operator.SHOULD) //
        .addFilteredQuery(QueryDtoHelper.createFilteredQuery(rangeDto)).build())) //
      .addFilteredQuery(QueryDtoHelper.createFilteredQuery(BoolFilterQueryDto.newBuilder() //
        .setOp(BoolFilterQueryDto.Operator.MUST_NOT) //
        .addFilteredQuery(QueryDtoHelper.createFilteredQuery(badTermsDto)).build())).build();

    FilteredQueryDto filteredDto = QueryDtoHelper.createFilteredQuery(boolDto);
    QueryDto queryDto = QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10).build();

    QueryDtoParser parser = QueryDtoParser.newParser();
    //System.out.println(parser.parse(queryDto).toString());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode expected = mapper.readTree(
      "{\"bool\":{\"must\":[{\"match_all\":{}},{\"bool\":{\"must\":[{\"terms\":{\"Study.populations.dataCollectionEvents.id\":[\"53f4b8ab6cf07b0996deb4f7\"]}},{\"bool\":{\"should\":{\"range\":{\"Study.populations.dataCollectionEvents.end\":{\"from\":\"2002\",\"to\":\"2012\",\"include_lower\":true,\"include_upper\":true}}}}},{\"bool\":{\"must_not\":{\"terms\":{\"Study.populations.dataCollectionEvents.id\":[\"aaaaaa\"]}}}}]}}]}}");
    JsonNode actual = mapper.readTree(parser.parse(queryDto).toString());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void test_query_dto_parser_logical_or_and() throws IOException {
    FieldFilterQueryDto termsDto = FieldFilterQueryDto.newBuilder()
      .setField("Study.populations.dataCollectionEvents.id").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("53f4b8ab6cf07b0996deb4f7")).build()).build();

    FieldFilterQueryDto rangeDto = FieldFilterQueryDto.newBuilder()
      .setField("Study.populations.dataCollectionEvents.end").setExtension(RangeFilterQueryDto.range,
        RangeFilterQueryDto.newBuilder()
          .setFrom(RangeConditionDto.newBuilder().setOp(RangeConditionDto.Operator.GTE).setValue("2002"))
          .setTo(RangeConditionDto.newBuilder().setOp(RangeConditionDto.Operator.LTE).setValue("2012")).build())
      .build();

    FieldFilterQueryDto badTermsDto = FieldFilterQueryDto.newBuilder()
      .setField("Study.populations.dataCollectionEvents.id").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("aaaaaa")).build()).build();

    LogicalFilterQueryDto logicalDto = LogicalFilterQueryDto.newBuilder() //
      .addFields(FieldStatementDto.newBuilder().setField(termsDto).setOp(FieldStatementDto.Operator._OR)) //
      .addFields(FieldStatementDto.newBuilder().setField(rangeDto).setOp(FieldStatementDto.Operator._AND)) //
      .addFields(FieldStatementDto.newBuilder().setField(badTermsDto)) //
      .build();

    FilteredQueryDto filteredDto = QueryDtoHelper.createFilteredQuery(logicalDto);
    QueryDto queryDto = QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10).build();

    QueryDtoParser parser = QueryDtoParser.newParser();
    System.out.println(parser.parse(queryDto).toString());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode expected = mapper.readTree(
      "{\"bool\":{\"must\":[{\"match_all\":{}},{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"terms\":{\"Study.populations.dataCollectionEvents.id\":[\"53f4b8ab6cf07b0996deb4f7\"]}},{\"range\":{\"Study.populations.dataCollectionEvents.end\":{\"from\":\"2002\",\"to\":\"2012\",\"include_lower\":true,\"include_upper\":true}}}]}},{\"terms\":{\"Study.populations.dataCollectionEvents.id\":[\"aaaaaa\"]}}]}}]}}");
    JsonNode actual = mapper.readTree(parser.parse(queryDto).toString());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void test_query_dto_parser_logical_or_and_not() throws IOException {
    FieldFilterQueryDto termsDto = FieldFilterQueryDto.newBuilder()
      .setField("Study.populations.dataCollectionEvents.id").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("53f4b8ab6cf07b0996deb4f7")).build()).build();

    FieldFilterQueryDto rangeDto = FieldFilterQueryDto.newBuilder()
      .setField("Study.populations.dataCollectionEvents.end").setExtension(RangeFilterQueryDto.range,
        RangeFilterQueryDto.newBuilder()
          .setFrom(RangeConditionDto.newBuilder().setOp(RangeConditionDto.Operator.GTE).setValue("2002"))
          .setTo(RangeConditionDto.newBuilder().setOp(RangeConditionDto.Operator.LTE).setValue("2012")).build())
      .build();

    FieldFilterQueryDto badTermsDto = FieldFilterQueryDto.newBuilder()
      .setField("Study.populations.dataCollectionEvents.id").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("aaaaaa")).build()).build();

    LogicalFilterQueryDto logicalDto = LogicalFilterQueryDto.newBuilder() //
      .addFields(FieldStatementDto.newBuilder().setField(termsDto).setOp(FieldStatementDto.Operator._OR)) //
      .addFields(FieldStatementDto.newBuilder().setField(rangeDto).setOp(FieldStatementDto.Operator._AND_NOT)) //
      .addFields(FieldStatementDto.newBuilder().setField(badTermsDto)) //
      .build();

    FilteredQueryDto filteredDto = QueryDtoHelper.createFilteredQuery(logicalDto);
    QueryDto queryDto = QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10).build();

    QueryDtoParser parser = QueryDtoParser.newParser();
    System.out.println(parser.parse(queryDto).toString());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode expected = mapper.readTree(
      "{\"bool\":{\"must\":[{\"match_all\":{}},{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"terms\":{\"Study.populations.dataCollectionEvents.id\":[\"53f4b8ab6cf07b0996deb4f7\"]}},{\"range\":{\"Study.populations.dataCollectionEvents.end\":{\"from\":\"2002\",\"to\":\"2012\",\"include_lower\":true,\"include_upper\":true}}}]}},{\"bool\":{\"must_not\":{\"terms\":{\"Study.populations.dataCollectionEvents.id\":[\"aaaaaa\"]}}}}]}}]}}");
    JsonNode actual = mapper.readTree(parser.parse(queryDto).toString());
    assertThat(actual).isEqualTo(expected);
  }

  @Configuration
  static class Config {}

}
