/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;

@Configuration
@EnableMetrics(proxyTargetClass = true)
public class MetricsConfiguration extends MetricsConfigurerAdapter implements EnvironmentAware {

  private static final String PROP_JMX_ENABLED = "metrics.jmx.enabled";

  private static final String PROP_GRAPHITE_ENABLED = "metrics.graphite.enabled";

  private static final String PROP_PORT = "metrics.graphite.port";

  private static final String PROP_HOST = "metrics.graphite.host";

  private static final String PROP_METRIC_REG_JVM_MEMORY = "jvm.memory";

  private static final String PROP_METRIC_REG_JVM_GARBAGE = "jvm.garbage";

  private static final String PROP_METRIC_REG_JVM_THREADS = "jvm.threads";

  private static final String PROP_METRIC_REG_JVM_FILES = "jvm.files";

  private static final String PROP_METRIC_REG_JVM_BUFFERS = "jvm.buffers";

  private static final Logger log = LoggerFactory.getLogger(MetricsConfiguration.class);

  private static final MetricRegistry METRIC_REGISTRY = new MetricRegistry();

  private static final HealthCheckRegistry HEALTH_CHECK_REGISTRY = new HealthCheckRegistry();

  private Environment environment;

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  @Bean
  public MetricRegistry getMetricRegistry() {
    return METRIC_REGISTRY;
  }

  @Override
  @Bean
  public HealthCheckRegistry getHealthCheckRegistry() {
    return HEALTH_CHECK_REGISTRY;
  }

  @PostConstruct
  public void init() {
    log.debug("Registering JVM gauges");
    METRIC_REGISTRY.register(PROP_METRIC_REG_JVM_MEMORY, new MemoryUsageGaugeSet());
    METRIC_REGISTRY.register(PROP_METRIC_REG_JVM_GARBAGE, new GarbageCollectorMetricSet());
    METRIC_REGISTRY.register(PROP_METRIC_REG_JVM_THREADS, new ThreadStatesGaugeSet());
    METRIC_REGISTRY.register(PROP_METRIC_REG_JVM_FILES, new FileDescriptorRatioGauge());
    METRIC_REGISTRY
        .register(PROP_METRIC_REG_JVM_BUFFERS, new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
    if(environment.getProperty(PROP_JMX_ENABLED, Boolean.class, false)) {
      log.info("Initializing Metrics JMX reporting");
      JmxReporter jmxReporter = JmxReporter.forRegistry(METRIC_REGISTRY).build();
      jmxReporter.start();
    }
  }

  @Configuration
  @ConditionalOnClass(Graphite.class)
  public static class GraphiteRegistry implements EnvironmentAware {

    private final Logger log = LoggerFactory.getLogger(GraphiteRegistry.class);

    @Inject
    private MetricRegistry metricRegistry;

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
      this.environment = environment;
    }

    @PostConstruct
    private void init() {
      Boolean graphiteEnabled = environment.getProperty(PROP_GRAPHITE_ENABLED, Boolean.class, false);
      if(graphiteEnabled) {
        log.info("Initializing Metrics Graphite reporting");
        String graphiteHost = environment.getRequiredProperty(PROP_HOST);
        Integer graphitePort = environment.getRequiredProperty(PROP_PORT, Integer.class);
        Graphite graphite = new Graphite(new InetSocketAddress(graphiteHost, graphitePort));
        GraphiteReporter graphiteReporter = GraphiteReporter.forRegistry(metricRegistry)
            .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build(graphite);
        graphiteReporter.start(1, TimeUnit.MINUTES);
      }
    }
  }
}
