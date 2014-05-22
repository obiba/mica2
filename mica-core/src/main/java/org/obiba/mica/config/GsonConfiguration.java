package org.obiba.mica.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Configuration
public class GsonConfiguration {

  @Bean
  public Gson gson() {
    return new GsonBuilder().create();
  }

}
