package org.obiba.mica.config;

import java.util.List;

import com.google.common.base.Splitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class RestConfig {

  @Value("${cors.allowedOrigins:}")
  private String  corsAllowedOrigins;

  @Value("${cors.allowedHeaders:}")
  private String  corsAllowedHeaders;

  @Value("${cors.allowedMethods:}")
  private String  corsAllowedMethods;

  @Value("${cors.allowCredentials:false}")
  private Boolean  corsAllowCredentials;

  @Value("${cors.maxAge:0}")
  private Long  corsMaxAge;

  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration configCors = new CorsConfiguration();

    configCors.setAllowCredentials(corsAllowCredentials);

    if(!corsAllowedOrigins.isEmpty()) {
      List<String> allowedOrigins = Splitter.on(",").splitToList(corsAllowedOrigins);
      allowedOrigins.forEach(configCors::addAllowedOrigin);
    }

    if(!corsAllowedHeaders.isEmpty()){
      List<String> allowedHeaders = Splitter.on(",").splitToList(corsAllowedHeaders);
      allowedHeaders.forEach(configCors::addAllowedHeader);
    }

    if(!corsAllowedMethods.isEmpty()){
      List<String> allowedMethods = Splitter.on(",").splitToList(corsAllowedMethods);
      allowedMethods.forEach(configCors::addAllowedMethod);
    }

    if(corsMaxAge!=0){
      configCors.setMaxAge(corsMaxAge);
    }

    source.registerCorsConfiguration("/**", configCors);
    return new CorsFilter(source);
    }

}
