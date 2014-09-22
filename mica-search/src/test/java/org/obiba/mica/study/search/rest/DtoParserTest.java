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
import org.obiba.mica.search.rest.QueryDtoParser;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.obiba.mica.web.model.MicaSearch.BoolFilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FilteredQueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryDto;
import static org.obiba.mica.web.model.MicaSearch.RangeConditionDto;
import static org.obiba.mica.web.model.MicaSearch.RangeFilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.TermsFilterQueryDto;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DtoParserTest.Config.class })
public class DtoParserTest {

  @Test
  public void test_query_dto_parser() throws IOException {
    FilterQueryDto termsDto1 = FilterQueryDto.newBuilder().setField("access").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("data", "bio-samples")).build()).build();

    FilterQueryDto termsDto2 = FilterQueryDto.newBuilder().setField("start").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("2002")).build()).build();

    BoolFilterQueryDto boolDto = BoolFilterQueryDto.newBuilder().addMust(termsDto1).addMust(termsDto2).build();
    FilteredQueryDto filteredDto = FilteredQueryDto.newBuilder().setFilter(boolDto).build();
    QueryDto quertDto = QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10).build();

    QueryDtoParser parser = QueryDtoParser.newParser();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node1 = mapper.readTree(
        "{\"filtered\": {\"query\": {\"match_all\": {} }, \"filter\": {\"bool\": {\"must\": [{\"terms\": {\"access\": [\"data\", \"bio-samples\"] } }, {\"terms\": {\"start\": [\"2002\"] } } ] } } } }");
    JsonNode node2 = mapper.readTree(parser.parse(quertDto).toString());
    assertThat(node1).isEqualTo(node2);
  }

  @Test
  public void test_query_dto_parser_parent_filter() throws IOException {
    // parent
    FilterQueryDto termsParentDto = FilterQueryDto.newBuilder().setField("id").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("53e28af784aed9e9db37be62")).build()).build();

    BoolFilterQueryDto boolParentDto = BoolFilterQueryDto.newBuilder().addMust(termsParentDto).build();
    FilteredQueryDto filteredParentDto = FilteredQueryDto.newBuilder().setFilter(boolParentDto).build();
    MicaSearch.ParentChildFilterDto parentChildFilterDto = MicaSearch.ParentChildFilterDto.newBuilder()
        .setType("Dataset").setRelationship(MicaSearch.ParentChildFilterDto.Relationship.PARENT)
        .setFilteredQuery(filteredParentDto).build();

    // child
    FilterQueryDto termsDto1 = FilterQueryDto.newBuilder().setField("valueType").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("integer")).build()).build();

    FilterQueryDto termsDto2 = FilterQueryDto.newBuilder().setField("attributes.maelstrom__measure.en")
        .setExtension(TermsFilterQueryDto.terms,
            TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("binary")).build()).build();

    BoolFilterQueryDto boolDto = BoolFilterQueryDto.newBuilder().addMust(termsDto1).addMust(termsDto2)
        .setParentChildFilter(parentChildFilterDto).build();
    FilteredQueryDto filteredDto = FilteredQueryDto.newBuilder().setFilter(boolDto).build();
    QueryDto quertDto = QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10).build();

    QueryDtoParser parser = QueryDtoParser.newParser();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node1 = mapper.readTree(
        "{\"filtered\":{\"query\":{\"match_all\":{}},\"filter\":{\"bool\":{\"must\":[{\"terms\":{\"valueType\":[\"integer\"]}},{\"terms\":{\"attributes.maelstrom__measure.en\":[\"binary\"]}},{\"has_parent\":{\"parent_type\":\"Dataset\",\"query\":{\"filtered\":{\"query\":{\"match_all\":{}},\"filter\":{\"bool\":{\"must\":{\"terms\":{\"id\":[\"53e28af784aed9e9db37be62\"]}}}}}}}}]}}}}");
    JsonNode node2 = mapper.readTree(parser.parse(quertDto).toString());
    assertThat(node1).isEqualTo(node2);
  }

  @Test
  public void test_query_dto_parser_child_filter() throws IOException {
    // child
    FilterQueryDto termsDto1 = FilterQueryDto.newBuilder().setField("valueType").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("integer")).build()).build();
    FilterQueryDto termsDto2 = FilterQueryDto.newBuilder().setField("attributes.maelstrom__measure.en")
        .setExtension(TermsFilterQueryDto.terms,
            TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("binary")).build()).build();
    BoolFilterQueryDto boolDto = BoolFilterQueryDto.newBuilder().addMust(termsDto1).addMust(termsDto2).build();
    FilteredQueryDto filteredParentDto = FilteredQueryDto.newBuilder().setFilter(boolDto).build();
    MicaSearch.ParentChildFilterDto parentChildFilterDto = MicaSearch.ParentChildFilterDto.newBuilder()
        .setType("Variable").setRelationship(MicaSearch.ParentChildFilterDto.Relationship.CHILD)
        .setFilteredQuery(filteredParentDto).build();

    // parent
    FilterQueryDto termsParentDto = FilterQueryDto.newBuilder().setField("id").setExtension(TermsFilterQueryDto.terms,
        TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("53e28af784aed9e9db37be62")).build()).build();
    BoolFilterQueryDto boolParentDto = BoolFilterQueryDto.newBuilder().addMust(termsParentDto)
        .setParentChildFilter(parentChildFilterDto).build();

    FilteredQueryDto filteredDto = FilteredQueryDto.newBuilder().setFilter(boolParentDto).build();
    QueryDto quertDto = QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10).build();

    QueryDtoParser parser = QueryDtoParser.newParser();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node1 = mapper.readTree(
        "{\"filtered\": {\"query\": {\"match_all\": {} }, \"filter\": {\"bool\": {\"must\": [{\"terms\": {\"id\": [\"53e28af784aed9e9db37be62\"] } }, {\"has_child\": {\"child_type\": \"Variable\", \"query\": {\"filtered\": {\"query\": {\"match_all\": {} }, \"filter\": {\"bool\": {\"must\": [{\"terms\": {\"valueType\": [\"integer\"] } }, {\"terms\": {\"attributes.maelstrom__measure.en\": [\"binary\"] } } ] } } } } } } ] } } } }");
    JsonNode node2 = mapper.readTree(parser.parse(quertDto).toString());
    assertThat(node1).isEqualTo(node2);
  }

  @Test
  public void test_query_dto_parser_terms_range_filter() throws IOException {
    FilterQueryDto termsDto = FilterQueryDto.newBuilder().setField("Study.populations.dataCollectionEvents.id")
        .setExtension(TermsFilterQueryDto.terms,
            TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("53f4b8ab6cf07b0996deb4f7")).build()).build();

    FilterQueryDto rangeDto = FilterQueryDto.newBuilder().setField("Study.populations.dataCollectionEvents.end")
        .setExtension(RangeFilterQueryDto.range, RangeFilterQueryDto.newBuilder()
                .setFrom(RangeConditionDto.newBuilder().setOp(RangeConditionDto.Operator.GTE).setValue("2002"))
                .setTo(RangeConditionDto.newBuilder().setOp(RangeConditionDto.Operator.LTE).setValue("2012")).build())
        .build();


    BoolFilterQueryDto boolDto = BoolFilterQueryDto.newBuilder().addMust(termsDto).addMust(rangeDto).build();
    FilteredQueryDto filteredDto = FilteredQueryDto.newBuilder().setFilter(boolDto).build();
    QueryDto quertDto = QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10).build();
    System.out.println(quertDto);
    QueryDtoParser parser = QueryDtoParser.newParser();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node1 = mapper.readTree(
        "{\"filtered\": {\"query\": {\"match_all\": {} }, \"filter\": {\"bool\": {\"must\": [{\"terms\": {\"Study.populations.dataCollectionEvents.id\": [\"53f4b8ab6cf07b0996deb4f7\"] } }, {\"range\": {\"Study.populations.dataCollectionEvents.end\": {\"from\": \"2002\", \"to\": \"2012\", \"include_lower\": true, \"include_upper\": true } } } ] } } } }");
    JsonNode node2 = mapper.readTree(parser.parse(quertDto).toString());
    assertThat(node1).isEqualTo(node2);
  }

  @Test
  public void test_query_dto_parser_bool_must_must_not_should() throws IOException {
    FilterQueryDto termsDto = FilterQueryDto.newBuilder().setField("Study.populations.dataCollectionEvents.id")
        .setExtension(TermsFilterQueryDto.terms,
            TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("53f4b8ab6cf07b0996deb4f7")).build()).build();

    FilterQueryDto rangeDto = FilterQueryDto.newBuilder().setField("Study.populations.dataCollectionEvents.end")
        .setExtension(RangeFilterQueryDto.range, RangeFilterQueryDto.newBuilder()
            .setFrom(RangeConditionDto.newBuilder().setOp(RangeConditionDto.Operator.GTE).setValue("2002"))
            .setTo(RangeConditionDto.newBuilder().setOp(RangeConditionDto.Operator.LTE).setValue("2012")).build())
        .build();

    FilterQueryDto badTermsDto = FilterQueryDto.newBuilder().setField("Study.populations.dataCollectionEvents.id")
        .setExtension(TermsFilterQueryDto.terms,
            TermsFilterQueryDto.newBuilder().addAllValues(Arrays.asList("aaaaaa")).build()).build();

    BoolFilterQueryDto boolDto = BoolFilterQueryDto.newBuilder().addMust(termsDto).addShould(rangeDto)
        .addMustNot(badTermsDto).build();
    FilteredQueryDto filteredDto = FilteredQueryDto.newBuilder().setFilter(boolDto).build();
    QueryDto quertDto = QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10).build();

    QueryDtoParser parser = QueryDtoParser.newParser();
    System.out.println(parser.parse(quertDto).toString());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node1 = mapper.readTree("{\"filtered\" : {\"query\" : {\"match_all\" : { } }, \"filter\" : {\"bool\" : {\"must\" : {\"terms\" : {\"Study.populations.dataCollectionEvents.id\" : [ \"53f4b8ab6cf07b0996deb4f7\" ] } }, \"must_not\" : {\"terms\" : {\"Study.populations.dataCollectionEvents.id\" : [ \"aaaaaa\" ] } }, \"should\" : {\"range\" : {\"Study.populations.dataCollectionEvents.end\" : {\"from\" : \"2002\", \"to\" : \"2012\", \"include_lower\" : true, \"include_upper\" : true } } } } } } }");
    JsonNode node2 = mapper.readTree(parser.parse(quertDto).toString());
    assertThat(node1).isEqualTo(node2);
  }

  @Configuration
  static class Config {}

}
