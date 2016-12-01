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
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class RQLQueryWrapperTest {

  @Test
  public void test_rql_query_terms_contains() throws IOException {
    String rql = "study(contains(Mica_study.populations-selectionCriteria-countriesIso,(CAN,USA)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    assertThat(boolQueryBuilder).isNotNull();
    assertThat(boolQueryBuilder.must()).isNotNull();
    List<QueryBuilder> queryBuilders = boolQueryBuilder.must();
    assertThat(queryBuilders).isNotNull();
    assertThat(queryBuilders.size()).isEqualTo(2);
    TermQueryBuilder termQueryBuilder = (TermQueryBuilder)queryBuilders.get(0);
    assertThat(termQueryBuilder).isNotNull();
    assertThat(termQueryBuilder.fieldName()).isEqualTo("Mica_study.populations-selectionCriteria-countriesIso");
    assertThat(termQueryBuilder.value()).isEqualTo("CAN");
    TermQueryBuilder termQueryBuilder1 = (TermQueryBuilder)queryBuilders.get(1);
    assertThat(termQueryBuilder1).isNotNull();
    assertThat(termQueryBuilder1.fieldName()).isEqualTo("Mica_study.populations-selectionCriteria-countriesIso");
    assertThat(termQueryBuilder1.value()).isEqualTo("USA");
  }

  @Test
  public void test_rql_query_terms_in() throws IOException {
    String rql
      = "variable(or(in(attributes.Mlstr_area__Lifestyle_behaviours.und,(Phys_act,Tobacco)),in(attributes.Mlstr_area__Diseases.und,Neoplasms)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    assertThat(boolQueryBuilder).isNotNull();
    assertThat(boolQueryBuilder.should()).isNotNull();
    List<QueryBuilder> queryBuilders = boolQueryBuilder.should();
    assertThat(queryBuilders).isNotNull();
    assertThat(queryBuilders.size()).isEqualTo(2);
    TermsQueryBuilder termsQueryBuilder = (TermsQueryBuilder)queryBuilders.get(0);
    assertThat(termsQueryBuilder).isNotNull();
    assertThat(termsQueryBuilder.fieldName()).isEqualTo("attributes.Mlstr_area__Lifestyle_behaviours.und");
    List<Object> innerValues = (List<Object>)termsQueryBuilder.values().get(0);
    assertThat(innerValues.size()).isEqualTo(2);
    assertThat(innerValues.indexOf("Phys_act")).isEqualTo(0);
    assertThat(innerValues.indexOf("Tobacco")).isEqualTo(1);
    TermsQueryBuilder termsQueryBuilder1 = (TermsQueryBuilder)queryBuilders.get(1);
    assertThat(termsQueryBuilder1).isNotNull();
    assertThat(termsQueryBuilder1.fieldName()).isEqualTo("attributes.Mlstr_area__Diseases.und");
    assertThat(termsQueryBuilder1.values().indexOf("Neoplasms")).isEqualTo(0);
  }

  @Test
  public void test_rql_query_terms_out() throws IOException {
    String rql = "variable(out(attributes.Mlstr_area__Diseases.und,Neoplasms))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    List<QueryBuilder> mustNots = boolQueryBuilder.mustNot();
    assertThat(mustNots).isNotNull();
    assertThat(mustNots.size()).isEqualTo(1);
    TermsQueryBuilder termsQueryBuilder = (TermsQueryBuilder) mustNots.get(0);
    assertThat(termsQueryBuilder).isNotNull();
    assertThat(termsQueryBuilder.fieldName()).isEqualTo("attributes.Mlstr_area__Diseases.und");
    assertThat(termsQueryBuilder.values().indexOf("Neoplasms")).isEqualTo(0);
  }

  @Test
  public void test_rql_query_terms_not_in() throws IOException {
    String rql = "variable(not(in(attributes.Mlstr_area__Diseases.und,Neoplasms)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    List<QueryBuilder> mustNots = boolQueryBuilder.mustNot();
    assertThat(mustNots).isNotNull();
    assertThat(mustNots.size()).isEqualTo(1);
    TermsQueryBuilder termsQueryBuilder = (TermsQueryBuilder) mustNots.get(0);
    assertThat(termsQueryBuilder).isNotNull();
    assertThat(termsQueryBuilder.fieldName()).isEqualTo("attributes.Mlstr_area__Diseases.und");
    assertThat(termsQueryBuilder.values().indexOf("Neoplasms")).isEqualTo(0);
  }

  @Test
  public void test_rql_query_range() throws IOException {
    String rql = "study(and(ge(populations.selectionCriteria.ageMin,50),le(populations.selectionCriteria.ageMin,60)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    List<QueryBuilder> musts = boolQueryBuilder.must();
    assertThat(musts).isNotNull();
    assertThat(musts.size()).isEqualTo(2);
    RangeQueryBuilder rangeAggregationBuilder = (RangeQueryBuilder) musts.get(0);
    assertThat(rangeAggregationBuilder).isNotNull();
    assertThat(rangeAggregationBuilder.fieldName()).isEqualTo("populations.selectionCriteria.ageMin");
    assertThat(rangeAggregationBuilder.from()).isEqualTo(50);
    assertThat(rangeAggregationBuilder.to()).isNull();
    assertThat(rangeAggregationBuilder.includeLower()).isTrue();
    assertThat(rangeAggregationBuilder.includeUpper()).isTrue();
    RangeQueryBuilder rangeAggregationBuilder1 = (RangeQueryBuilder) musts.get(1);
    assertThat(rangeAggregationBuilder1).isNotNull();
    assertThat(rangeAggregationBuilder1.fieldName()).isEqualTo("populations.selectionCriteria.ageMin");
    assertThat(rangeAggregationBuilder1.from()).isNull();
    assertThat(rangeAggregationBuilder1.to()).isEqualTo(60);
    assertThat(rangeAggregationBuilder1.includeLower()).isTrue();
    assertThat(rangeAggregationBuilder1.includeUpper()).isTrue();
  }

  @Test
  public void test_rql_query_match() throws IOException {
    String rql = "variable(match(tutu))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder)rqlQueryWrapper.getQueryBuilder();
    assertThat(queryStringQueryBuilder).isNotNull();
    assertThat(queryStringQueryBuilder.queryString()).isEqualTo("tutu");
  }

  @Test
  public void test_rql_query_exists() throws IOException {
    String rql = "variable(exists(tutu))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    ExistsQueryBuilder existsQueryBuilder = (ExistsQueryBuilder)rqlQueryWrapper.getQueryBuilder();
    assertThat(existsQueryBuilder).isNotNull();
    assertThat(existsQueryBuilder.fieldName()).isEqualTo("tutu");
  }

  @Test
  public void test_rql_query_missing() throws IOException {
    String rql = "variable(missing(tutu))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    assertThat(boolQueryBuilder).isNotNull();
    assertThat(boolQueryBuilder.mustNot()).isNotNull();
    assertThat(boolQueryBuilder.mustNot().size()).isEqualTo(1);
    assertThat(boolQueryBuilder.mustNot().get(0) instanceof ExistsQueryBuilder).isTrue();
    assertThat(((ExistsQueryBuilder)boolQueryBuilder.mustNot().get(0)).fieldName()).isEqualTo("tutu");
  }

  @Test
  public void test_rql_query_not_match() throws IOException {
    String rql = "variable(not(match(tutu)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    assertThat(boolQueryBuilder).isNotNull();
    assertThat(boolQueryBuilder.mustNot()).isNotNull();
    QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder) boolQueryBuilder.mustNot().get(0);
    assertThat(queryStringQueryBuilder).isNotNull();
    assertThat(queryStringQueryBuilder.queryString()).isEqualTo("tutu");
  }

  @Test
  public void test_rql_query_nand() throws IOException {
    String rql
      = "variable(nand(in(Mlstr_area.Lifestyle_behaviours,Alcohol),in(Mlstr_area.Diseases,Neoplasms)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    assertThat(boolQueryBuilder).isNotNull();
    assertThat(boolQueryBuilder.mustNot()).isNotNull();
    List<QueryBuilder> queryBuilders = boolQueryBuilder.mustNot();
    assertThat(queryBuilders).isNotNull();
    assertThat(queryBuilders.size()).isEqualTo(1);
    BoolQueryBuilder boolQueryBuilder1 = (BoolQueryBuilder) queryBuilders.get(0);
    List<QueryBuilder> queryBuilders1 = boolQueryBuilder1.must();
    assertThat(queryBuilders1).isNotNull();
    assertThat(queryBuilders1.size()).isEqualTo(2);
    TermsQueryBuilder termsQueryBuilder = (TermsQueryBuilder)queryBuilders1.get(0);
    assertThat(termsQueryBuilder).isNotNull();
    assertThat(termsQueryBuilder.fieldName()).isEqualTo("Mlstr_area.Lifestyle_behaviours");
    assertThat(termsQueryBuilder.values().indexOf("Alcohol")).isEqualTo(0);
    TermsQueryBuilder termsQueryBuilder1 = (TermsQueryBuilder)queryBuilders1.get(1);
    assertThat(termsQueryBuilder1).isNotNull();
    assertThat(termsQueryBuilder1.fieldName()).isEqualTo("Mlstr_area.Diseases");
    assertThat(termsQueryBuilder1.values().indexOf("Neoplasms")).isEqualTo(0);
  }

  @Test
  public void test_rql_query_nor() throws IOException {
    String rql
      = "variable(nor(in(Mlstr_area.Lifestyle_behaviours,Alcohol),in(Mlstr_area.Diseases,Neoplasms)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    assertThat(boolQueryBuilder).isNotNull();
    assertThat(boolQueryBuilder.mustNot()).isNotNull();
    List<QueryBuilder> queryBuilders = boolQueryBuilder.mustNot();
    assertThat(queryBuilders).isNotNull();
    assertThat(queryBuilders.size()).isEqualTo(1);
    BoolQueryBuilder boolQueryBuilder1 = (BoolQueryBuilder) queryBuilders.get(0);
    List<QueryBuilder> queryBuilders1 = boolQueryBuilder1.should();
    assertThat(queryBuilders1).isNotNull();
    assertThat(queryBuilders1.size()).isEqualTo(2);
    TermsQueryBuilder termsQueryBuilder = (TermsQueryBuilder)queryBuilders1.get(0);
    assertThat(termsQueryBuilder).isNotNull();
    assertThat(termsQueryBuilder.fieldName()).isEqualTo("Mlstr_area.Lifestyle_behaviours");
    assertThat(termsQueryBuilder.values().indexOf("Alcohol")).isEqualTo(0);
    TermsQueryBuilder termsQueryBuilder1 = (TermsQueryBuilder)queryBuilders1.get(1);
    assertThat(termsQueryBuilder1).isNotNull();
    assertThat(termsQueryBuilder1.fieldName()).isEqualTo("Mlstr_area.Diseases");
    assertThat(termsQueryBuilder1.values().indexOf("Neoplasms")).isEqualTo(0);
  }

  @Test
  public void test_rql_query_complex_match() throws IOException {
    String rql = "variable(match(name:tutu description:tata pwel))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "name:tutu description:tata pwel";

    assertThat(((QueryStringQueryBuilder) rqlQueryWrapper.getQueryBuilder()).queryString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_match_with_field() throws IOException {
    String rql = "variable(match(tutu,name))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    assertThat(queryStringQueryBuilder.queryString()).isEqualTo("tutu");
    assertThat(queryStringQueryBuilder.fields().get("name")).isNotNull();
  }

  @Test
  public void test_rql_query_match_with_fields() throws IOException {
    String rql = "variable(match(tutu,(name,description)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    assertThat(queryStringQueryBuilder.queryString()).isEqualTo("tutu");
    Map<String, Float> fields = queryStringQueryBuilder.fields();
    assertThat(fields.get("name")).isNotNull();
    assertThat(fields.get("description")).isNotNull();
  }

  @Test
  public void test_rql_query_between() throws IOException {
    String rql = "study(between(populations.selectionCriteria.ageMin,(50,60)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    RangeQueryBuilder rangeQueryBuilder = (RangeQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    assertThat(rangeQueryBuilder.from()).isEqualTo(50);
    assertThat(rangeQueryBuilder.to()).isEqualTo(60);
    assertThat(rangeQueryBuilder.includeLower()).isTrue();
    assertThat(rangeQueryBuilder.includeUpper()).isFalse();
    assertThat(rangeQueryBuilder.boost()).isEqualTo(1F);
  }

  @Test
  public void test_rql_query_not_between() throws IOException {
    String rql = "study(not(between(populations.selectionCriteria.ageMin,(50,60))))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) rqlQueryWrapper.getQueryBuilder();
    assertThat(boolQueryBuilder.boost()).isEqualTo(1F);
    assertThat(boolQueryBuilder.disableCoord()).isFalse();
    assertThat(boolQueryBuilder.adjustPureNegative()).isTrue();
    List<QueryBuilder> mustNots = boolQueryBuilder.mustNot();
    assertThat(mustNots).isNotNull();
    assertThat(mustNots.size()).isEqualTo(1);
    RangeQueryBuilder rangeAggregationBuilder = (RangeQueryBuilder) mustNots.get(0);
    assertThat(rangeAggregationBuilder).isNotNull();
    assertThat(rangeAggregationBuilder.fieldName()).isEqualTo("populations.selectionCriteria.ageMin");
    assertThat(rangeAggregationBuilder.from()).isEqualTo(50);
    assertThat(rangeAggregationBuilder.to()).isEqualTo(60);
    assertThat(rangeAggregationBuilder.includeLower()).isTrue();
    assertThat(rangeAggregationBuilder.includeUpper()).isFalse();
    assertThat(rangeAggregationBuilder.boost()).isEqualTo(1F);
  }

  @Test
  public void test_rql_query_term_and_limit_and_sort() throws IOException {
    String rql = "network(eq(id,ialsa),limit(3,4),sort(-name))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expectedQuery = "{\n" +
      "  \"term\" : {\n" +
      "    \"id\" : {\n" +
      "      \"value\" : \"ialsa\",\n" +
      "      \"boost\" : 1.0\n" + "    }\n" +
      "  }\n" +
      "}";

    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expectedQuery);
    assertThat(rqlQueryWrapper.getFrom()).isEqualTo(3);
    assertThat(rqlQueryWrapper.getSize()).isEqualTo(4);
    String expectedSort = "{\n" +
      "  \"name\" : {\n" +
      "    \"order\" : \"desc\"\n" +
      "  }\n" +
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
