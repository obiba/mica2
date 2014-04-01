package org.obiba.mica.config;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.h2.server.web.WebServlet;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.jboss.resteasy.plugins.spring.SpringContextLoaderSupport;
import org.obiba.mica.web.filter.CachingHttpHeadersFilter;
import org.obiba.mica.web.filter.StaticResourcesProductionFilter;
import org.obiba.mica.web.filter.gzip.GZipServletFilter;
import org.obiba.mica.web.rest.RestConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.MetricsServlet;

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
@Configuration
@AutoConfigureAfter(CacheConfiguration.class)
public class WebConfigurer implements ServletContextInitializer {

  private static final Logger log = LoggerFactory.getLogger(WebConfigurer.class);

  @Inject
  private Environment env;

  @Inject
  private MetricRegistry metricRegistry;

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    log.info("Web application configuration, using profiles: {}", Arrays.toString(env.getActiveProfiles()));

    initRestEasy(servletContext);

    EnumSet<DispatcherType> disps = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC);
    initMetrics(servletContext, disps);
    if(env.acceptsProfiles(Constants.SPRING_PROFILE_PRODUCTION)) {
      initStaticResourcesProductionFilter(servletContext, disps);
      initCachingHttpHeadersFilter(servletContext, disps);
    }
    initGzipFilter(servletContext, disps);
    initH2Console(servletContext);

    log.info("Web application fully configured");
  }

  private void initRestEasy(ServletContext servletContext) {

    servletContext.setInitParameter("resteasy.servlet.mapping.prefix", RestConfigurer.WS_ROOT);

    servletContext.addListener(ResteasyBootstrap.class.getName());
    servletContext.addListener(Spring4ContextLoaderListener.class.getName());

    ServletRegistration.Dynamic resteasyServlet = servletContext.addServlet("resteasy", HttpServletDispatcher.class);
    resteasyServlet.addMapping(RestConfigurer.WS_ROOT + "/*");
  }

  /**
   * Initializes the GZip filter.
   */
  private void initGzipFilter(ServletContext servletContext, EnumSet<DispatcherType> disps) {
    log.debug("Registering GZip Filter");

    FilterRegistration.Dynamic compressingFilter = servletContext.addFilter("gzipFilter", new GZipServletFilter());
    Map<String, String> parameters = new HashMap<>();

    compressingFilter.setInitParameters(parameters);

    compressingFilter.addMappingForUrlPatterns(disps, true, "*.css");
    compressingFilter.addMappingForUrlPatterns(disps, true, "*.json");
    compressingFilter.addMappingForUrlPatterns(disps, true, "*.html");
    compressingFilter.addMappingForUrlPatterns(disps, true, "*.js");
    compressingFilter.addMappingForUrlPatterns(disps, true, "/ws/*");
    compressingFilter.addMappingForUrlPatterns(disps, true, "/metrics/*");

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
   * Initializes the cachig HTTP Headers Filter.
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

  /**
   * Initializes H2 console
   */
  private void initH2Console(ServletContext servletContext) {
    log.debug("Initialize H2 console");
    ServletRegistration.Dynamic h2ConsoleServlet = servletContext.addServlet("H2Console", new WebServlet());
    h2ConsoleServlet.addMapping("/console/*");
    h2ConsoleServlet.setLoadOnStartup(1);
  }

  // https://issues.jboss.org/browse/RESTEASY-1012
  public static class Spring4ContextLoaderListener extends ContextLoaderListener {

    private final SpringContextLoaderSupport springContextLoaderSupport = new SpringContextLoaderSupport();

    @Override
    protected void customizeContext(ServletContext servletContext,
        ConfigurableWebApplicationContext configurableWebApplicationContext) {
      super.customizeContext(servletContext, configurableWebApplicationContext);
      springContextLoaderSupport.customizeContext(servletContext, configurableWebApplicationContext);
    }
  }

}
