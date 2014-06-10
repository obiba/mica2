package org.obiba.mica.search;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ElasticSearchService implements EnvironmentAware {

  private static final Logger log = LoggerFactory.getLogger(ElasticSearchService.class);

  public static final String PATH_DATA = "${MICA_SERVER_HOME}/work/elasticsearch/data";

  public static final String PATH_WORK = "${MICA_SERVER_HOME}/work/elasticsearch/work";

  private RelaxedPropertyResolver propertyResolver;

  @Nullable
  private Node node;

  @Nullable
  private Client client;

  @Override
  public void setEnvironment(Environment environment) {
    propertyResolver = new RelaxedPropertyResolver(environment, "elasticsearch.");
  }

  @PostConstruct
  public void start() {
    if(isRunning()) {
      throw new IllegalStateException("Cannot start Elastic Search as it is already running.");
    }
    log.debug("Starting Elastic Search node");
    String micaHome = System.getProperty("MICA_SERVER_HOME");
    node = NodeBuilder.nodeBuilder() //
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
    client = node.client();
  }

  @PreDestroy
  public void stop() {
    if(node != null) {
      node.close();
      node = null;
      client = null;
    }
  }

  public boolean isRunning() {
    return node != null;
  }

  @NotNull
  public Client getClient() {
    if(client == null) {
      throw new IllegalStateException("Elastic Search is not running!");
    }
    return client;
  }

  @NotNull
  public RestController getRest() {
    if(node == null) {
      throw new IllegalStateException("Elastic Search is not running!");
    }
    return ((InternalNode) node).injector().getInstance(RestController.class);
  }

  public int getNbShards() {
    return propertyResolver.getProperty("shards", Integer.class, 5);
  }

  public int getNbReplicas() {
    return propertyResolver.getProperty("replicas", Integer.class, 1);
  }

}
