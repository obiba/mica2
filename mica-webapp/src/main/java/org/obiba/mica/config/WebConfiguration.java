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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.MetricsServlet;
import com.google.common.base.Strings;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.obiba.mica.web.filter.CachingHttpHeadersFilter;
import org.obiba.mica.web.filter.ClickjackingHttpHeadersFilter;
import org.obiba.mica.web.filter.StaticResourcesProductionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;

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

  private static final int MAX_IDLE_TIME = 30000;

  private static final int REQUEST_HEADER_SIZE = 8192;

  private Environment environment;

  private final org.obiba.ssl.SslContextFactory sslContextFactory;

  private int httpsPort;

  private String serverAddress;

  private String contextPath;

  @Inject
  public WebConfiguration(org.obiba.ssl.SslContextFactory sslContextFactory) {
    this.sslContextFactory = sslContextFactory;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
    httpsPort = environment.getProperty("https.port", Integer.class, DEFAULT_HTTPS_PORT);
    serverAddress = environment.getProperty("server.address", "localhost");
    contextPath = environment.getProperty("server.context-path", "");
    if (Strings.isNullOrEmpty(contextPath))
      contextPath = environment.getProperty("server.servlet.context-path", "");
  }

  @Bean
  public WebServerFactoryCustomizer<JettyServletWebServerFactory> containerCustomizer() throws Exception {
    WebConfiguration that = this;

    return new WebServerFactoryCustomizer<JettyServletWebServerFactory>() {

      @Override
      public void customize(JettyServletWebServerFactory factory) {
        factory.setServerCustomizers(Arrays.asList(that));
        if (!Strings.isNullOrEmpty(contextPath) && contextPath.startsWith("/")) factory.setContextPath(contextPath);
      }
    };
  }

  @Override
  public void customize(Server server) {
    customizeSsl(server);

    GzipHandler gzipHandler = new GzipHandler();
    gzipHandler.setIncludedMethods("PUT", "POST", "GET");
    gzipHandler.setInflateBufferSize(2048);
    gzipHandler.setHandler(server.getHandler());
    server.setHandler(gzipHandler);
  }

  private void customizeSsl(Server server) {
    if (httpsPort <= 0) return;

    SslContextFactory jettySsl = new SslContextFactory() {

      @Override
      protected void doStart() throws Exception {
        setSslContext(sslContextFactory.createSslContext());
        super.doStart();
      }
    };
    jettySsl.setWantClientAuth(true);
    jettySsl.setNeedClientAuth(false);
    jettySsl.addExcludeProtocols("SSL", "SSLv2", "SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1");

    ServerConnector sslConnector = new ServerConnector(server, jettySsl);
    sslConnector.setHost(serverAddress);
    sslConnector.setPort(httpsPort);
    sslConnector.setIdleTimeout(MAX_IDLE_TIME);

    server.addConnector(sslConnector);
  }

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    log.info("Web application configuration, using profiles: {}", Arrays.toString(environment.getActiveProfiles()));

    servletContext.addListener(EnvironmentLoaderListener.class);

    log.info("Web application fully configured");
  }

  @Bean
  public FreeMarkerViewResolver freemarkerViewResolver() {
    FreeMarkerViewResolver freeMarkerViewResolver = new FreeMarkerViewResolver();
    freeMarkerViewResolver.setRequestContextAttribute("rc");
    freeMarkerViewResolver.setSuffix(".ftl");
    freeMarkerViewResolver.setContentType("text/html;charset=UTF-8");

    return freeMarkerViewResolver;
  }

  @Bean
  public FreeMarkerConfigurer freeMarkerConfigurer() {
    FreeMarkerConfigurer freeMarkerConfigurer = new FreeMarkerConfigurer();
    freeMarkerConfigurer.setDefaultEncoding("UTF-8");
    freeMarkerConfigurer.setTemplateLoaderPaths("classpath:/web/", "classpath:/static/templates/", "classpath:/public/templates/", "classpath:/templates/", "classpath:/_templates/");

    return freeMarkerConfigurer;
  }

  @Bean
  public FilterRegistrationBean<InstrumentedFilter> instrumentedFilterRegistration() {
    log.debug("Registering Instrumented Filter");
    FilterRegistrationBean<InstrumentedFilter> bean = new FilterRegistrationBean<>();

    bean.setFilter(new InstrumentedFilter());
    bean.addUrlPatterns("/*");
    bean.setAsyncSupported(true);

    return bean;
  }

  @Bean
  public ServletRegistrationBean<MetricsServlet> metricsServlet(MetricRegistry metricRegistry) {
    log.debug("Registering Metrics Servlet");
    ServletRegistrationBean<MetricsServlet> bean = new ServletRegistrationBean<>();

    bean.setServlet(new MetricsServlet(metricRegistry));
    bean.addUrlMappings("/metrics/metrics/*");
    bean.setAsyncSupported(true);
    bean.setLoadOnStartup(2);

    return bean;
  }

  @Bean
  public FilterRegistrationBean<ClickjackingHttpHeadersFilter> clickjackingHttpHeadersFilterRegistration() {
    log.debug("Registering Click Jacking Http Header Filter");
    FilterRegistrationBean<ClickjackingHttpHeadersFilter> bean = new FilterRegistrationBean<>();

    bean.setFilter(new ClickjackingHttpHeadersFilter());
    bean.addUrlPatterns("/*");
    bean.setAsyncSupported(true);

    return bean;
  }

  @Bean
  @Profile({"prod"})
  public FilterRegistrationBean<StaticResourcesProductionFilter> staticResourcesProductionFilterRegistration() {
    log.debug("Registering Static Resources Production Filter");
    FilterRegistrationBean<StaticResourcesProductionFilter> bean = new FilterRegistrationBean<>();

    bean.setFilter(new StaticResourcesProductionFilter());
    bean.addUrlPatterns("/favicon.ico");
    bean.addUrlPatterns("/robots.txt");
    bean.addUrlPatterns("/index.html");
    bean.addUrlPatterns("/images/*");
    bean.addUrlPatterns("/fonts/*");
    bean.addUrlPatterns("/scripts/*");
    bean.addUrlPatterns("/styles/*");
    bean.addUrlPatterns("/views/*");
    bean.setAsyncSupported(true);

    return bean;
  }

  @Bean
  @Profile({"prod"})
  public FilterRegistrationBean<CachingHttpHeadersFilter> cachingHttpHeadersFilterRegistration() {
    log.debug("Registering Caching Htpp Headers Filter");
    FilterRegistrationBean<CachingHttpHeadersFilter> bean = new FilterRegistrationBean<>();

    bean.setFilter(new CachingHttpHeadersFilter());
    bean.addUrlPatterns("/images/*");
    bean.addUrlPatterns("/fonts/*");
    bean.addUrlPatterns("/scripts/*");
    bean.addUrlPatterns("/styles/*");
    bean.setAsyncSupported(true);

    return bean;
  }

  @Bean
  public FilterRegistrationBean<CharacterEncodingFilter> CharacterEncodingFilterRegistration() {
    FilterRegistrationBean<CharacterEncodingFilter> bean = new FilterRegistrationBean<>();
    CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();

    characterEncodingFilter.setEncoding("UTF-8");
    characterEncodingFilter.setForceResponseEncoding(true);

    bean.setFilter(characterEncodingFilter);
    bean.addUrlPatterns("/*");
    bean.setAsyncSupported(true);
    return bean;
  }

  @Bean
  public FilterRegistrationBean<NoTraceFilter> noTraceFilterRegistration() {
    log.debug("Registering No Trace Filter");
    FilterRegistrationBean<NoTraceFilter> bean = new FilterRegistrationBean<>();

    bean.setFilter(new NoTraceFilter());
    bean.addUrlPatterns("/*");
    bean.setAsyncSupported(true);

    return bean;
  }

  @Bean
  public FilterRegistrationBean<ForbiddenUrlsFilter> forbiddenUrlsFilterRegistration() {
    log.debug("Registering Forbidden Urls Filter");
    FilterRegistrationBean<ForbiddenUrlsFilter> bean = new FilterRegistrationBean<>();

    bean.setFilter(new ForbiddenUrlsFilter());
    bean.addUrlPatterns("/.htaccess");
    bean.addUrlPatterns("/.htaccess/");
    bean.setAsyncSupported(true);

    return bean;
  }

  /**
   * When a TRACE request is received, returns a Forbidden response.
   */
  private static class NoTraceFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      if("TRACE".equals(httpRequest.getMethod())) {
        httpResponse.reset();
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "TRACE method not allowed");
        return;
      }
      chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
  }

  /**
   * Filters all forbidden URLs registered above (/.htaccess, /.htacces/)
   */
  private static class ForbiddenUrlsFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      httpResponse.reset();
      httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, String.format("%s not allowed", httpRequest.getRequestURI()));
    }

    @Override
    public void destroy() {
    }
  }
}
