package org.obiba.mica.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.obiba.mica.web.filter.CachingHttpHeadersFilter;
import org.obiba.mica.web.filter.StaticResourcesProductionFilter;
import org.obiba.mica.web.filter.gzip.GZipServletFilter;
import org.obiba.shiro.web.filter.AuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.MetricsServlet;

import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.ERROR;
import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.INCLUDE;
import static javax.servlet.DispatcherType.REQUEST;
import static org.obiba.mica.config.JerseyConfiguration.WS_ROOT;

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
@Configuration
@ComponentScan({ "org.obiba.mica", "org.obiba.shiro" })
@PropertySource("classpath:mica-webapp.properties")
@AutoConfigureAfter(SecurityConfiguration.class)
public class WebConfiguration implements ServletContextInitializer, JettyServerCustomizer, EnvironmentAware {

  private static final Logger log = LoggerFactory.getLogger(WebConfiguration.class);

  private static final int DEFAULT_HTTPS_PORT = 8445;

  private static final String DEFAULT_KEYSTORE_FILE = "file:${MICA_SERVER_HOME}/conf/keystore.p12";

  private static final String DEFAULT_KEYSTORE_TYPE = "PKCS12";

  private static final int MAX_IDLE_TIME = 30000;

  private static final int REQUEST_HEADER_SIZE = 8192;

  @Inject
  private Environment env;

  @Inject
  private MetricRegistry metricRegistry;

  @Inject
  private AuthenticationFilter authenticationFilter;

  private int httpsPort;

  private Resource keystoreFile;

  private String keystorePass;

  private String keystoreType;

  @Override
  public void setEnvironment(Environment environment) {
    RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment, "https.");
    httpsPort = propertyResolver.getProperty("port", Integer.class, DEFAULT_HTTPS_PORT);

    ResourceEditor resourceEditor = new ResourceEditor();
    resourceEditor.setAsText(propertyResolver.getProperty("keystore.file", String.class, DEFAULT_KEYSTORE_FILE));
    keystoreFile = (Resource) resourceEditor.getValue();

    keystorePass = propertyResolver.getProperty("keystore.password");
    keystoreType = propertyResolver.getProperty("keystore.type", String.class, DEFAULT_KEYSTORE_TYPE);
  }

  @Bean
  EmbeddedServletContainerCustomizer containerCustomizer() throws Exception {
    return (ConfigurableEmbeddedServletContainer container) -> {
      JettyEmbeddedServletContainerFactory jetty = (JettyEmbeddedServletContainerFactory) container;
      jetty.setServerCustomizers(Collections.singleton(this));
    };
  }

  @Override
  public void customize(Server server) {
    customizeSsl(server);
  }

  private void customizeSsl(Server server) {
    SslContextFactory jettySsl = new SslContextFactory();
    jettySsl.setWantClientAuth(true);
    jettySsl.setNeedClientAuth(false);

    try {
      jettySsl.setKeyStorePath(keystoreFile.getFile().getAbsolutePath());
      jettySsl.setKeyStorePassword(keystorePass);
      jettySsl.setKeyStoreType(keystoreType);
    } catch(IOException e) {
      throw new RuntimeException("Failed to set jetty server keystore: " + e.getMessage(), e);
    }

    Connector sslConnector = new SslSelectChannelConnector(jettySsl);
    sslConnector.setPort(httpsPort);
    sslConnector.setMaxIdleTime(MAX_IDLE_TIME);
    sslConnector.setRequestHeaderSize(REQUEST_HEADER_SIZE);

    server.addConnector(sslConnector);
  }

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    log.info("Web application configuration, using profiles: {}", Arrays.toString(env.getActiveProfiles()));

    initAuthenticationFilter(servletContext);

    EnumSet<DispatcherType> disps = EnumSet.of(REQUEST, FORWARD, ASYNC);
    initMetrics(servletContext, disps);
    if(env.acceptsProfiles(Profiles.PROD)) {
      initStaticResourcesProductionFilter(servletContext, disps);
      initCachingHttpHeadersFilter(servletContext, disps);
    }
    initGzipFilter(servletContext, disps);

    log.info("Web application fully configured");
  }

  private void initAuthenticationFilter(ServletContext servletContext) {
    log.debug("Registering Authentication Filter");
    FilterRegistration.Dynamic filterRegistration = servletContext
        .addFilter("authenticationFilter", authenticationFilter);
    filterRegistration
        .addMappingForUrlPatterns(EnumSet.of(REQUEST, FORWARD, ASYNC, INCLUDE, ERROR), true, WS_ROOT + "/*");
    filterRegistration.setAsyncSupported(true);
  }

  /**
   * Initializes the GZip filter.
   */
  private void initGzipFilter(ServletContext servletContext, EnumSet<DispatcherType> disps) {
    log.debug("Registering GZip Filter");

    FilterRegistration.Dynamic compressingFilter = servletContext.addFilter("gzipFilter", new GZipServletFilter());
    compressingFilter.addMappingForUrlPatterns(disps, true, "*.css");
    compressingFilter.addMappingForUrlPatterns(disps, true, "*.json");
    compressingFilter.addMappingForUrlPatterns(disps, true, "*.html");
    compressingFilter.addMappingForUrlPatterns(disps, true, "*.js");
    compressingFilter.addMappingForUrlPatterns(disps, true, "/metrics/*");
    // don't compress WS_ROOT/* or Jersey ExceptionMapper won't work
    compressingFilter.setAsyncSupported(true);
  }

  /**
   * Initializes the static resources production Filter.
   */
  private void initStaticResourcesProductionFilter(ServletContext servletContext, EnumSet<DispatcherType> disps) {

    log.debug("Registering static resources production Filter");
    FilterRegistration.Dynamic resourcesFilter = servletContext
        .addFilter("staticResourcesProductionFilter", new StaticResourcesProductionFilter());

    resourcesFilter.addMappingForUrlPatterns(disps, true, "/");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/index.html");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/images/*");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/fonts/*");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/scripts/*");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/styles/*");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/views/*");
    resourcesFilter.setAsyncSupported(true);
  }

  /**
   * Initializes the caching HTTP Headers Filter.
   */
  private void initCachingHttpHeadersFilter(ServletContext servletContext, EnumSet<DispatcherType> disps) {
    log.debug("Registering Caching HTTP Headers Filter");
    FilterRegistration.Dynamic cachingFilter = servletContext
        .addFilter("cachingHttpHeadersFilter", new CachingHttpHeadersFilter());

    cachingFilter.addMappingForUrlPatterns(disps, true, "/images/*");
    cachingFilter.addMappingForUrlPatterns(disps, true, "/fonts/*");
    cachingFilter.addMappingForUrlPatterns(disps, true, "/scripts/*");
    cachingFilter.addMappingForUrlPatterns(disps, true, "/styles/*");
    cachingFilter.setAsyncSupported(true);
  }

  /**
   * Initializes Metrics.
   */
  private void initMetrics(ServletContext servletContext, EnumSet<DispatcherType> disps) {
    log.debug("Initializing Metrics registries");
    servletContext.setAttribute(InstrumentedFilter.REGISTRY_ATTRIBUTE, metricRegistry);
    servletContext.setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);

    log.debug("Registering Metrics Filter");
    FilterRegistration.Dynamic metricsFilter = servletContext
        .addFilter("webappMetricsFilter", new InstrumentedFilter());

    metricsFilter.addMappingForUrlPatterns(disps, true, "/*");
    metricsFilter.setAsyncSupported(true);

    log.debug("Registering Metrics Servlet");
    ServletRegistration.Dynamic metricsAdminServlet = servletContext.addServlet("metricsServlet", new MetricsServlet());

    metricsAdminServlet.addMapping("/metrics/metrics/*");
    metricsAdminServlet.setAsyncSupported(true);
    metricsAdminServlet.setLoadOnStartup(2);
  }

}
