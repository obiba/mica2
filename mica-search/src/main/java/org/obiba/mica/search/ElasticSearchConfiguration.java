package org.obiba.mica.search;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ElasticSearchConfiguration implements EnvironmentAware {

  private static final Logger log = LoggerFactory.getLogger(ElasticSearchConfiguration.class);

  public static final String PATH_DATA = "${MICA_SERVER_HOME}/work/elasticsearch/data";

  public static final String PATH_WORK = "${MICA_SERVER_HOME}/work/elasticsearch/work";

  private RelaxedPropertyResolver propertyResolver;

  @Override
  public void setEnvironment(Environment environment) {
    propertyResolver = new RelaxedPropertyResolver(environment, "elasticsearch.");
  }

  @Bean
  public Client client() {
    String micaHome = System.getProperty("MICA_SERVER_HOME");
    Node node = NodeBuilder.nodeBuilder() //
        .client(!propertyResolver.getProperty("dataNode", Boolean.class, true)) //
        .settings(ImmutableSettings.settingsBuilder() //
            .classLoader(getClass().getClassLoader()) //
            .loadFromClasspath("elasticsearch.yml") //
            .put("path.data", PATH_DATA.replace("${MICA_SERVER_HOME}", micaHome)) //
            .put("path.work", PATH_WORK.replace("${MICA_SERVER_HOME}", micaHome)) //
            .loadFromSource(propertyResolver.getProperty("settings")) //
        ) //
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

}
