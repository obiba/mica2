package org.obiba.mica.web.rest;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource("classpath:springmvc-resteasy.xml")
@SuppressWarnings("ClassMayBeInterface")
public class RestConfigurer {

  public static final String WS_ROOT = "/ws";

}
