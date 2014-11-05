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

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.mica.search.rest.QueryDtoHelper;
import org.obiba.mica.search.rest.QueryDtoParser;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.obiba.mica.web.model.MicaSearch.BoolFilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FieldFilterQueryDto;
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
    JsonNode node1 = mapper.readTree(
      "{\"filtered\": {\"query\": {\"match_all\": {} }, \"filter\": {\"bool\": {\"must\": [{\"terms\": {\"access\": [\"data\", \"bio-samples\"] } }, {\"terms\": {\"start\": [\"2002\"] } } ] } } } }");
    JsonNode node2 = mapper.readTree(parser.parse(queryDto).toString());
    assertThat(node1).isEqualTo(node2);
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
    JsonNode expected = mapper.readTree("{\n" +
      "  \"filtered\" : {\n" +
      "    \"query\" : {\n" +
      "      \"match_all\" : { }\n" +
      "    },\n" +
      "    \"filter\" : {\n" +
      "      \"bool\" : {\n" +
      "        \"must\" : [ {\n" +
      "          \"terms\" : {\n" +
      "            \"Study.populations.dataCollectionEvents.id\" : [ \"53f4b8ab6cf07b0996deb4f7\" ]\n" +
      "          }\n" +
      "        }, {\n" +
      "          \"range\" : {\n" +
      "            \"Study.populations.dataCollectionEvents.end\" : {\n" +
      "              \"from\" : \"2002\",\n" +
      "              \"to\" : \"2012\",\n" +
      "              \"include_lower\" : true,\n" +
      "              \"include_upper\" : true\n" +
      "            }\n" +
      "          }\n" +
      "        } ]\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}");
    JsonNode actual = mapper.readTree(parser.parse(queryDto).toString());
    assertThat(expected).isEqualTo(actual);
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
    JsonNode expected = mapper.readTree("{\n" +
      "  \"filtered\" : {\n" +
      "    \"query\" : {\n" +
      "      \"match_all\" : { }\n" +
      "    },\n" +
      "    \"filter\" : {\n" +
      "      \"bool\" : {\n" +
      "        \"must\" : [ {\n" +
      "          \"terms\" : {\n" +
      "            \"Study.populations.dataCollectionEvents.id\" : [ \"53f4b8ab6cf07b0996deb4f7\" ]\n" +
      "          }\n" +
      "        }, {\n" +
      "          \"bool\" : {\n" +
      "            \"should\" : {\n" +
      "              \"range\" : {\n" +
      "                \"Study.populations.dataCollectionEvents.end\" : {\n" +
      "                  \"from\" : \"2002\",\n" +
      "                  \"to\" : \"2012\",\n" +
      "                  \"include_lower\" : true,\n" +
      "                  \"include_upper\" : true\n" +
      "                }\n" +
      "              }\n" +
      "            }\n" +
      "          }\n" +
      "        }, {\n" +
      "          \"bool\" : {\n" +
      "            \"must_not\" : {\n" +
      "              \"terms\" : {\n" +
      "                \"Study.populations.dataCollectionEvents.id\" : [ \"aaaaaa\" ]\n" +
      "              }\n" +
      "            }\n" +
      "          }\n" +
      "        } ]\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}");
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
      .addFields(LogicalFilterQueryDto.FieldStatementDto.newBuilder().setField(termsDto)
        .setOp(LogicalFilterQueryDto.Operator._OR)) //
      .addFields(LogicalFilterQueryDto.FieldStatementDto.newBuilder().setField(rangeDto).setOp(
        LogicalFilterQueryDto.Operator._AND)) //
      .addFields(LogicalFilterQueryDto.FieldStatementDto.newBuilder().setField(badTermsDto)) //
      .build();

    FilteredQueryDto filteredDto = QueryDtoHelper.createFilteredQuery(logicalDto);
    QueryDto queryDto = QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10).build();

    QueryDtoParser parser = QueryDtoParser.newParser();
    System.out.println(parser.parse(queryDto).toString());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode expected = mapper.readTree("{\n" +
      "  \"filtered\" : {\n" +
      "    \"query\" : {\n" +
      "      \"match_all\" : { }\n" +
      "    },\n" +
      "    \"filter\" : {\n" +
      "      \"bool\" : {\n" +
      "        \"must\" : [ {\n" +
      "          \"bool\" : {\n" +
      "            \"should\" : [ {\n" +
      "              \"terms\" : {\n" +
      "                \"Study.populations.dataCollectionEvents.id\" : [ \"53f4b8ab6cf07b0996deb4f7\" ]\n" +
      "              }\n" +
      "            }, {\n" +
      "              \"range\" : {\n" +
      "                \"Study.populations.dataCollectionEvents.end\" : {\n" +
      "                  \"from\" : \"2002\",\n" +
      "                  \"to\" : \"2012\",\n" +
      "                  \"include_lower\" : true,\n" +
      "                  \"include_upper\" : true\n" +
      "                }\n" +
      "              }\n" +
      "            } ]\n" +
      "          }\n" +
      "        }, {\n" +
      "          \"terms\" : {\n" +
      "            \"Study.populations.dataCollectionEvents.id\" : [ \"aaaaaa\" ]\n" +
      "          }\n" +
      "        } ]\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}");
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
      .addFields(LogicalFilterQueryDto.FieldStatementDto.newBuilder().setField(termsDto)
        .setOp(LogicalFilterQueryDto.Operator._OR)) //
      .addFields(LogicalFilterQueryDto.FieldStatementDto.newBuilder().setField(rangeDto).setOp(
        LogicalFilterQueryDto.Operator._AND_NOT)) //
      .addFields(LogicalFilterQueryDto.FieldStatementDto.newBuilder().setField(badTermsDto)) //
      .build();

    FilteredQueryDto filteredDto = QueryDtoHelper.createFilteredQuery(logicalDto);
    QueryDto queryDto = QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10).build();

    QueryDtoParser parser = QueryDtoParser.newParser();
    System.out.println(parser.parse(queryDto).toString());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode expected = mapper.readTree("{\n" +
      "  \"filtered\" : {\n" +
      "    \"query\" : {\n" +
      "      \"match_all\" : { }\n" +
      "    },\n" +
      "    \"filter\" : {\n" +
      "      \"bool\" : {\n" +
      "        \"must\" : [ {\n" +
      "          \"bool\" : {\n" +
      "            \"should\" : [ {\n" +
      "              \"terms\" : {\n" +
      "                \"Study.populations.dataCollectionEvents.id\" : [ \"53f4b8ab6cf07b0996deb4f7\" ]\n" +
      "              }\n" +
      "            }, {\n" +
      "              \"range\" : {\n" +
      "                \"Study.populations.dataCollectionEvents.end\" : {\n" +
      "                  \"from\" : \"2002\",\n" +
      "                  \"to\" : \"2012\",\n" +
      "                  \"include_lower\" : true,\n" +
      "                  \"include_upper\" : true\n" +
      "                }\n" +
      "              }\n" +
      "            } ]\n" +
      "          }\n" +
      "        }, {\n" +
      "          \"bool\" : {\n" +
      "            \"must_not\" : {\n" +
      "              \"terms\" : {\n" +
      "                \"Study.populations.dataCollectionEvents.id\" : [ \"aaaaaa\" ]\n" +
      "              }\n" +
      "            }\n" +
      "          }\n" +
      "        } ]\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}");
    JsonNode actual = mapper.readTree(parser.parse(queryDto).toString());
    assertThat(actual).isEqualTo(expected);
  }

  @Configuration
  static class Config {}

}
