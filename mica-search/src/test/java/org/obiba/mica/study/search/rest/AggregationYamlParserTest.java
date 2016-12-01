/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.mica.search.aggregations.AggregationYamlParser;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { AggregationYamlParserTest.Config.class })
public class AggregationYamlParserTest {

  private static Client client;

  private static AggregationYamlParser aggregationYamlParser = new AggregationYamlParser();

  private static Path dataDirectory;

  @BeforeClass
  public static void beforeClass() throws IOException, NodeValidationException {
    dataDirectory = Files.createTempDirectory("es-test", new FileAttribute<?>[] {});
    aggregationYamlParser.setLocales(Arrays.asList(Locale.ENGLISH, Locale.FRENCH));
    client = newNode(dataDirectory.toString()).client();
  }

  @AfterClass
  public static void afterClass() throws IOException {
    FileUtils.deleteDirectory(dataDirectory.toFile());
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
    SearchRequestBuilder requestBuilder = client.prepareSearch("test_index") //
      .setTypes("test_type") //
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
      .setQuery(QueryBuilders.matchAllQuery());

    aggregationYamlParser.getAggregations(new ClassPathResource(resource), null)
      .forEach(requestBuilder::addAggregation);

    return getRequestAsJSon(requestBuilder).get("aggregations");
  }

  private static Node newNode(String dataDirectory) throws NodeValidationException {
    Node node = new Node(Settings.builder() //
      .put("transport.type", "local")
      .put("node.data", false)
      .put("cluster.name", nodeName()) //
      .put("node.name", nodeName()) //
//      .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1) //
//      .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0) //
      .put("processors", 1) // limit the number of threads created
      .put("http.enabled", false) //
      .put("index.store.type", "ram") //
//      .put("config.ignore_system_properties", true) // make sure we get what we set :)
      .put("path.home", "/tmp") //
      .put("path.data", dataDirectory)
      .build()//
    );


    //IndexSettings
    node.start();
//    assertThat(localNode(node.settings())).isTrue();
    return node;
  }

  private JsonNode getRequestAsJSon(SearchRequestBuilder requestBuilder) throws IOException {
    return new ObjectMapper().readTree(requestBuilder.toString());
  }

  private static String nodeName() {
    return AggregationYamlParserTest.class.getName();
  }

  @Configuration
  @ComponentScan("org.obiba.mica.study.search.rest")
  static class Config {}

}
