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
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.obiba.mica.search.aggregations.AggregationYamlParser;
import org.obiba.mica.support.TestElasticSearchClient;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AggregationYamlParserTest {

  private static TestElasticSearchClient client = new TestElasticSearchClient();

  private static AggregationYamlParser aggregationYamlParser = new AggregationYamlParser();

  @BeforeClass
  public static void beforeClass() throws IOException {
    client.init();
    aggregationYamlParser.setLocales(Arrays.asList(Locale.ENGLISH, Locale.FRENCH));
  }

  @AfterClass
  public static void afterClass() throws IOException {
    client.cleanup();
  }

  @Test
  public void test_invalid_type() throws URISyntaxException, IOException {
    JsonNode node = getJsonNode("invalid-type.yml").get("agg");
    assertThat(node).isNotNull();
    assertThat(node.get("terms")).isNotNull();
  }

  @Test
  public void test_invalid_localized_value() throws IOException {
    JsonNode node = getJsonNode("invalid-localized-value.yml").get("agg");
    assertThat(node).isNotNull();
    assertThat(node.get("terms")).isNotNull();
  }

  @Test
  public void test_valid_aggs() throws IOException {
    JsonNode node = getJsonNode("aggregations.yml");
    JsonNode normal = node.get("normal");
    assertThat(normal).isNotNull();
    assertThat(normal.get("terms")).isNotNull();

    JsonNode number = node.get("number");
    assertThat(number).isNotNull();
    assertThat(number.get("stats")).isNotNull();

    JsonNode text_en = node.get("text-en");
    assertThat(text_en).isNotNull();
    assertThat(text_en.get("terms")).isNotNull();

    JsonNode text_fr = node.get("text-fr");
    assertThat(text_fr).isNotNull();
    assertThat(text_fr.get("terms")).isNotNull();

    JsonNode text_und = node.get("text-und");
    assertThat(text_und).isNotNull();
    assertThat(text_und.get("terms")).isNotNull();

    JsonNode parent_child = node.get("parent-child");
    assertThat(parent_child).isNotNull();
    assertThat(parent_child.get("terms")).isNotNull();
  }

  @Test
  public void test_invalid_localized_stats_agg() throws IOException {
    System.out.println(getJsonNode("invalid-localized-stats-agg.yml"));
    JsonNode agg_en = getJsonNode("invalid-localized-stats-agg.yml").get("agg-en");
    assertThat(agg_en).isNotNull();
    assertThat(agg_en.get("terms")).isNotNull();

    JsonNode agg_fr = getJsonNode("invalid-localized-stats-agg.yml").get("agg-fr");
    assertThat(agg_fr).isNotNull();
    assertThat(agg_fr.get("terms")).isNotNull();
  }

  @Test
  public void test_aggs_with_aliases() throws IOException {
    JsonNode agg_alias = getJsonNode("aggregation-with-alias.yml");
    System.out.println(agg_alias.toString());
    assertThat(agg_alias).isNotNull();
    assertThat(agg_alias.get("textId")).isNotNull();
    assertThat(agg_alias.get("text2-id")).isNotNull();
    assertThat(agg_alias.get("text3-id")).isNotNull();
  }

  private JsonNode getJsonNode(String resource) throws IOException {
    SearchRequestBuilder requestBuilder = client.preSearchRequest(QueryBuilders.matchAllQuery());
    aggregationYamlParser.getAggregations(new ClassPathResource(resource), null)
      .forEach(requestBuilder::addAggregation);

    return getRequestAsJSon(requestBuilder).get("aggregations");
  }

  private JsonNode getRequestAsJSon(SearchRequestBuilder requestBuilder) throws IOException {
    return new ObjectMapper().readTree(requestBuilder.toString());
  }
}
