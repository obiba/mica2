/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Stream;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.google.common.base.Throwables;

import static java.util.stream.Collectors.toList;

@Configuration
public class ElasticSearchConfiguration implements EnvironmentAware {

  private static final Logger log = LoggerFactory.getLogger(ElasticSearchConfiguration.class);

  private static final String ES_CONFIG_FILE = "elasticsearch.yml";

  public static final String PATH_DATA = "${MICA_HOME}/work/elasticsearch/data";

  public static final String PATH_WORK = "${MICA_HOME}/work/elasticsearch/work";

  private RelaxedPropertyResolver propertyResolver;

  @Override
  public void setEnvironment(Environment environment) {
    propertyResolver = new RelaxedPropertyResolver(environment, "elasticsearch.");
  }

  @Bean
  public Client client() {
    return Boolean.parseBoolean(propertyResolver.getProperty("transportClient"))
      ? createTransportClient()
      : createNodeClient();
  }

  private Client createTransportClient() {
    Settings.Builder settingsBuilder = Settings.settingsBuilder() //
      .put("cluster.name", propertyResolver.getProperty("clusterName")) //
      .put("client.transport.sniff", Boolean.parseBoolean(propertyResolver.getProperty("transportSniff")));
    List<String> transportAddresses = Stream.of(propertyResolver.getProperty("transportAddress", "").split(","))
      .map(String::trim).collect(toList());

    TransportClient client = TransportClient.builder().settings(settingsBuilder.build()).build();

    transportAddresses.forEach(ta -> {
      int port = 9300;
      String host = ta;
      int sepIdx = ta.lastIndexOf(':');

      if(sepIdx > 0) {
        port = Integer.parseInt(ta.substring(sepIdx + 1, ta.length()));
        host = ta.substring(0, sepIdx);
      }

      try {
        client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));
      } catch(UnknownHostException e) {
        Throwables.propagate(e);
      }
    });

    return client;
  }

  private Client createNodeClient() {
    Node node = NodeBuilder.nodeBuilder() //
      .client(!propertyResolver.getProperty("dataNode", Boolean.class, true)) //
      .settings(getSettings()) //
      .clusterName(propertyResolver.getProperty("clusterName", "mica")) //
      .node();

    return node.client();
  }

  public int getNbShards() {
    return propertyResolver.getProperty("shards", Integer.class, 5);
  }

  public int getNbReplicas() {
    return propertyResolver.getProperty("replicas", Integer.class, 1);
  }

  public Settings getIndexSettings () {
    return getSettings().getByPrefix("index.");
  }

  private Settings getSettings() {
    String micaHome = System.getProperty("MICA_HOME");
    InputStream is = getClass().getClassLoader().getResourceAsStream(ES_CONFIG_FILE);

    return Settings.builder() //
      .loadFromStream(ES_CONFIG_FILE, is) //
      .put("path.data", PATH_DATA.replace("${MICA_HOME}", micaHome)) //
      .put("path.home", micaHome) //
      .put("path.work", PATH_WORK.replace("${MICA_HOME}", micaHome)) //
      .loadFromSource(propertyResolver.getProperty("settings")).build();
  }
}
