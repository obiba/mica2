/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DtoParserTest.Config.class })
public class DtoParserTest {

  @Test
  public void test_query_dto_parser() throws IOException {
    MicaSearch.TermsQueryDto termsDto1 = MicaSearch.TermsQueryDto.newBuilder().setField("access")
        .addAllValues(Arrays.asList("data", "bio-samples")).build();

    MicaSearch.TermsQueryDto termsDto2 = MicaSearch.TermsQueryDto.newBuilder().setField("start")
        .addAllValues(Arrays.asList("2002")).build();

    MicaSearch.BoolFilterQueryDto boolDto = MicaSearch.BoolFilterQueryDto.newBuilder().addTerms(termsDto1)
        .addTerms(termsDto2).build();
    MicaSearch.FilteredQueryDto filteredDto = MicaSearch.FilteredQueryDto.newBuilder().setFilter(boolDto).build();
    MicaSearch.QueryDto quertDto = MicaSearch.QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10)
        .setDetailed(false).build();

    QueryDtoParser parser = QueryDtoParser.newParser();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node1 = mapper.readTree("{\"filtered\": {\"query\": {\"match_all\": {} }, \"filter\": {\"bool\": {\"must\": [{\"terms\": {\"access\": [\"data\", \"bio-samples\"] } }, {\"terms\": {\"start\": [\"2002\"] } } ] } } } }");
    JsonNode node2 = mapper.readTree(parser.parse(quertDto).toString());
    assertThat(node1).isEqualTo(node2);
  }

  @Test
  public void test_query_dto_parser_parent_filter() throws IOException {
    // parent
    MicaSearch.TermsQueryDto termsParentDto = MicaSearch.TermsQueryDto.newBuilder().setField("id")
        .addAllValues(Arrays.asList("53e28af784aed9e9db37be62")).build();
    MicaSearch.BoolFilterQueryDto boolParentDto = MicaSearch.BoolFilterQueryDto.newBuilder().addTerms(termsParentDto)
        .build();
    MicaSearch.FilteredQueryDto filteredParentDto = MicaSearch.FilteredQueryDto.newBuilder().setFilter(boolParentDto)
        .build();
    MicaSearch.ParentChildFilterDto parentChildFilterDto = MicaSearch.ParentChildFilterDto.newBuilder()
        .setType("Dataset").setRelationship(MicaSearch.ParentChildFilterDto.Relationship.PARENT)
        .setFilteredQuery(filteredParentDto).build();

    // child
    MicaSearch.TermsQueryDto termsDto1 = MicaSearch.TermsQueryDto.newBuilder().setField("valueType")
        .addAllValues(Arrays.asList("integer")).build();
    MicaSearch.TermsQueryDto termsDto2 = MicaSearch.TermsQueryDto.newBuilder()
        .setField("attributes.maelstrom__measure.en").addAllValues(Arrays.asList("binary")).build();
    MicaSearch.BoolFilterQueryDto boolDto = MicaSearch.BoolFilterQueryDto.newBuilder().addTerms(termsDto1)
        .addTerms(termsDto2).setParentChildFilter(parentChildFilterDto).build();
    MicaSearch.FilteredQueryDto filteredDto = MicaSearch.FilteredQueryDto.newBuilder().setFilter(boolDto).build();
    MicaSearch.QueryDto quertDto = MicaSearch.QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10)
        .setDetailed(false).build();

    QueryDtoParser parser = QueryDtoParser.newParser();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node1 = mapper.readTree("{\"filtered\":{\"query\":{\"match_all\":{}},\"filter\":{\"bool\":{\"must\":[{\"terms\":{\"valueType\":[\"integer\"]}},{\"terms\":{\"attributes.maelstrom__measure.en\":[\"binary\"]}},{\"has_parent\":{\"parent_type\":\"Dataset\",\"query\":{\"filtered\":{\"query\":{\"match_all\":{}},\"filter\":{\"bool\":{\"must\":{\"terms\":{\"id\":[\"53e28af784aed9e9db37be62\"]}}}}}}}}]}}}}");
    JsonNode node2 = mapper.readTree(parser.parse(quertDto).toString());
    assertThat(node1).isEqualTo(node2);
  }


  @Test
  public void test_query_dto_parser_child_filter() throws IOException {
    // child
    MicaSearch.TermsQueryDto termsDto1 = MicaSearch.TermsQueryDto.newBuilder().setField("valueType")
        .addAllValues(Arrays.asList("integer")).build();
    MicaSearch.TermsQueryDto termsDto2 = MicaSearch.TermsQueryDto.newBuilder()
        .setField("attributes.maelstrom__measure.en").addAllValues(Arrays.asList("binary")).build();
    MicaSearch.BoolFilterQueryDto boolDto = MicaSearch.BoolFilterQueryDto.newBuilder().addTerms(termsDto1).addTerms(termsDto2)
        .build();
    MicaSearch.FilteredQueryDto filteredParentDto = MicaSearch.FilteredQueryDto.newBuilder().setFilter(boolDto)
        .build();
    MicaSearch.ParentChildFilterDto parentChildFilterDto = MicaSearch.ParentChildFilterDto.newBuilder()
        .setType("Variable").setRelationship(MicaSearch.ParentChildFilterDto.Relationship.CHILD)
        .setFilteredQuery(filteredParentDto).build();


    // parent
    MicaSearch.TermsQueryDto termsParentDto = MicaSearch.TermsQueryDto.newBuilder().setField("id")
        .addAllValues(Arrays.asList("53e28af784aed9e9db37be62")).build();
    MicaSearch.BoolFilterQueryDto boolParentDto = MicaSearch.BoolFilterQueryDto.newBuilder().addTerms(termsParentDto)
        .setParentChildFilter(parentChildFilterDto).build();

    MicaSearch.FilteredQueryDto filteredDto = MicaSearch.FilteredQueryDto.newBuilder().setFilter(boolParentDto).build();
    MicaSearch.QueryDto quertDto = MicaSearch.QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10)
        .setDetailed(false).build();

    System.out.println(quertDto);

    QueryDtoParser parser = QueryDtoParser.newParser();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node1 = mapper.readTree("{\"filtered\": {\"query\": {\"match_all\": {} }, \"filter\": {\"bool\": {\"must\": [{\"terms\": {\"id\": [\"53e28af784aed9e9db37be62\"] } }, {\"has_child\": {\"child_type\": \"Variable\", \"query\": {\"filtered\": {\"query\": {\"match_all\": {} }, \"filter\": {\"bool\": {\"must\": [{\"terms\": {\"valueType\": [\"integer\"] } }, {\"terms\": {\"attributes.maelstrom__measure.en\": [\"binary\"] } } ] } } } } } } ] } } } }");
    JsonNode node2 = mapper.readTree(parser.parse(quertDto).toString());
    assertThat(node1).isEqualTo(node2);
  }

  @Configuration
  static class Config {}

}
