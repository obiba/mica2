/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries.rql;

import java.io.IOException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class RQLQueryWrapperTest {

  @Test
  public void test_rql_query_terms_contains() throws IOException {
    String rql
      = "study(contains(Mica_study.populations-selectionCriteria-countriesIso,(CAN,USA)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"bool\" : {\n" +
      "    \"must\" : [ {\n" +
      "      \"term\" : {\n" +
      "        \"Mica_study.populations-selectionCriteria-countriesIso\" : \"CAN\"\n" +
      "      }\n" +
      "    }, {\n" +
      "      \"term\" : {\n" +
      "        \"Mica_study.populations-selectionCriteria-countriesIso\" : \"USA\"\n" +
      "      }\n" +
      "    } ]\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_terms_in() throws IOException {
    String rql
      = "variable(or(in(attributes.Mlstr_area__Lifestyle_behaviours.und,(Phys_act,Tobacco)),in(attributes.Mlstr_area__Diseases.und,Neoplasms)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"bool\" : {\n" +
      "    \"should\" : [ {\n" +
      "      \"terms\" : {\n" +
      "        \"attributes.Mlstr_area__Lifestyle_behaviours.und\" : [ [ \"Phys_act\", \"Tobacco\" ] ]\n" +
      "      }\n" +
      "    }, {\n" +
      "      \"terms\" : {\n" +
      "        \"attributes.Mlstr_area__Diseases.und\" : [ \"Neoplasms\" ]\n" +
      "      }\n" +
      "    } ]\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_terms_out() throws IOException {
    String rql
      = "variable(out(attributes.Mlstr_area__Diseases.und,Neoplasms))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"bool\" : {\n" +
      "    \"must_not\" : {\n" +
      "      \"terms\" : {\n" +
      "        \"attributes.Mlstr_area__Diseases.und\" : [ \"Neoplasms\" ]\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_terms_not_in() throws IOException {
    String rql
      = "variable(not(in(attributes.Mlstr_area__Diseases.und,Neoplasms)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"bool\" : {\n" +
      "    \"must_not\" : {\n" +
      "      \"terms\" : {\n" +
      "        \"attributes.Mlstr_area__Diseases.und\" : [ \"Neoplasms\" ]\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_range() throws IOException {
    String rql = "study(and(ge(populations.selectionCriteria.ageMin,50),le(populations.selectionCriteria.ageMin,60)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"bool\" : {\n" +
      "    \"must\" : [ {\n" +
      "      \"range\" : {\n" +
      "        \"populations.selectionCriteria.ageMin\" : {\n" +
      "          \"from\" : 50,\n" +
      "          \"to\" : null,\n" +
      "          \"include_lower\" : true,\n" +
      "          \"include_upper\" : true\n" +
      "        }\n" +
      "      }\n" +
      "    }, {\n" +
      "      \"range\" : {\n" +
      "        \"populations.selectionCriteria.ageMin\" : {\n" +
      "          \"from\" : null,\n" +
      "          \"to\" : 60,\n" +
      "          \"include_lower\" : true,\n" +
      "          \"include_upper\" : true\n" +
      "        }\n" +
      "      }\n" +
      "    } ]\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_match() throws IOException {
    String rql
      = "variable(match(tutu))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"query_string\" : {\n" +
      "    \"query\" : \"tutu\"\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_exists() throws IOException {
    String rql
      = "variable(exists(tutu))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"exists\" : {\n" +
      "    \"field\" : \"tutu\"\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_missing() throws IOException {
    String rql
      = "variable(missing(tutu))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"missing\" : {\n" +
      "    \"field\" : \"tutu\"\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_not_match() throws IOException {
    String rql
      = "variable(not(match(tutu)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"bool\" : {\n" +
      "    \"must_not\" : {\n" +
      "      \"query_string\" : {\n" +
      "        \"query\" : \"tutu\"\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_nand() throws IOException {
    String rql
      = "variable(nand(in(Mlstr_area.Lifestyle_behaviours,Alcohol),in(Mlstr_area.Diseases,Neoplasms)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"bool\" : {\n" +
      "    \"must_not\" : {\n" +
      "      \"bool\" : {\n" +
      "        \"must\" : [ {\n" +
      "          \"terms\" : {\n" +
      "            \"Mlstr_area.Lifestyle_behaviours\" : [ \"Alcohol\" ]\n" +
      "          }\n" +
      "        }, {\n" +
      "          \"terms\" : {\n" +
      "            \"Mlstr_area.Diseases\" : [ \"Neoplasms\" ]\n" +
      "          }\n" +
      "        } ]\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_nor() throws IOException {
    String rql
      = "variable(nor(in(Mlstr_area.Lifestyle_behaviours,Alcohol),in(Mlstr_area.Diseases,Neoplasms)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"bool\" : {\n" +
      "    \"must_not\" : {\n" +
      "      \"bool\" : {\n" +
      "        \"should\" : [ {\n" +
      "          \"terms\" : {\n" +
      "            \"Mlstr_area.Lifestyle_behaviours\" : [ \"Alcohol\" ]\n" +
      "          }\n" +
      "        }, {\n" +
      "          \"terms\" : {\n" +
      "            \"Mlstr_area.Diseases\" : [ \"Neoplasms\" ]\n" +
      "          }\n" +
      "        } ]\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_complex_match() throws IOException {
    String rql
      = "variable(match(name:tutu description:tata pwel))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"query_string\" : {\n" +
      "    \"query\" : \"name:tutu description:tata pwel\"\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_match_with_field() throws IOException {
    String rql
      = "variable(match(tutu,name))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"query_string\" : {\n" +
      "    \"query\" : \"tutu\",\n" +
      "    \"fields\" : [ \"name\" ]\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_match_with_fields() throws IOException {
    String rql
      = "variable(match(tutu,(name,description)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"query_string\" : {\n" +
      "    \"query\" : \"tutu\",\n" +
      "    \"fields\" : [ \"name\", \"description\" ]\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_between() throws IOException {
    String rql = "study(between(populations.selectionCriteria.ageMin,(50,60)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"range\" : {\n" +
      "    \"populations.selectionCriteria.ageMin\" : {\n" +
      "      \"from\" : 50,\n" +
      "      \"to\" : 60,\n" +
      "      \"include_lower\" : true,\n" +
      "      \"include_upper\" : false\n" +
      "    }\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_not_between() throws IOException {
    String rql = "study(not(between(populations.selectionCriteria.ageMin,(50,60))))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"bool\" : {\n" +
      "    \"must_not\" : {\n" +
      "      \"range\" : {\n" +
      "        \"populations.selectionCriteria.ageMin\" : {\n" +
      "          \"from\" : 50,\n" +
      "          \"to\" : 60,\n" +
      "          \"include_lower\" : true,\n" +
      "          \"include_upper\" : false\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_term_and_limit_and_sort() throws IOException {
    String rql = "network(eq(id,ialsa),limit(3,4),sort(-name))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expectedQuery = "{\n" +
      "  \"term\" : {\n" +
      "    \"id\" : \"ialsa\"\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expectedQuery);
    assertThat(rqlQueryWrapper.getFrom()).isEqualTo(3);
    assertThat(rqlQueryWrapper.getSize()).isEqualTo(4);
    String expectedSort = "\n" +
      "\"name\"{\n" +
      "  \"order\" : \"desc\"\n" +
      "}";
    assertThat(rqlQueryWrapper.getSortBuilder().toString()).isEqualTo(expectedSort);
  }

  @Test
  public void test_rql_query_aggregation() throws IOException {
    String rql = "variable(aggregate(Mlstr_area.Lifestyle_behaviours,Mlstr_area.Diseases))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.getAggregations()).isNotNull();
    assertThat(rqlQueryWrapper.getAggregations().size()).isEqualTo(2);
    assertThat(rqlQueryWrapper.getAggregations().get(0)).isEqualTo("Mlstr_area.Lifestyle_behaviours");
    assertThat(rqlQueryWrapper.getAggregations().get(1)).isEqualTo("Mlstr_area.Diseases");
  }

  @Test
  public void test_rql_query_aggregation_bucket() throws IOException {
    String rql = "variable(aggregate(Mlstr_area.Lifestyle_behaviours,Mlstr_area.Diseases,bucket(studyId,datasetId)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.getAggregations()).isNotNull();
    assertThat(rqlQueryWrapper.getAggregations().size()).isEqualTo(2);
    assertThat(rqlQueryWrapper.getAggregations().get(0)).isEqualTo("Mlstr_area.Lifestyle_behaviours");
    assertThat(rqlQueryWrapper.getAggregations().get(1)).isEqualTo("Mlstr_area.Diseases");
    assertThat(rqlQueryWrapper.getAggregationBuckets()).isNotNull();
    assertThat(rqlQueryWrapper.getAggregationBuckets().size()).isEqualTo(2);
    assertThat(rqlQueryWrapper.getAggregationBuckets().get(0)).isEqualTo("studyId");
    assertThat(rqlQueryWrapper.getAggregationBuckets().get(1)).isEqualTo("datasetId");
  }

  @Test
  public void test_query_argument_type() throws IOException {
    String rql = "study(in(Mica_study.id,3d)";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    assertThat(rqlQueryWrapper.getNode().getArgument(1)).isEqualTo("3d");
  }
}
