/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class TestElasticSearchClient {

  private Client client;

  private Path dataDirectory;

  public void init() throws IOException {
    dataDirectory = Files.createTempDirectory("es-test", new FileAttribute<?>[] {});
    client = newNode(dataDirectory.toString()).client();
  }

  public void cleanup() throws IOException {
    FileUtils.deleteDirectory(dataDirectory.toFile());
  }

  public SearchRequestBuilder preSearchRequest(QueryBuilder queryBuilder) {
    return client.prepareSearch("test_index") //
      .setTypes("test_type") //
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
      .setQuery(queryBuilder);
  }

  private static Node newNode(String dataDirectory) {
    String nodeName = TestElasticSearchClient.class.getName();
    Node build = NodeBuilder.nodeBuilder().local(true).data(false).settings(Settings.builder() //
      .put(ClusterName.SETTING, nodeName) //
      .put("node.name", nodeName) //
      .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1) //
      .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0) //
      .put(EsExecutors.PROCESSORS, 1) // limit the number of threads created
      .put("http.enabled", false) //
      .put("index.store.type", "ram") //
      .put("config.ignore_system_properties", true) // make sure we get what we set :)
      .put("path.home", "/tmp") //
      .put("path.data", dataDirectory) //
    ).build();

    build.start();
    assertThat(DiscoveryNode.localNode(build.settings())).isTrue();
    return build;
  }



}
