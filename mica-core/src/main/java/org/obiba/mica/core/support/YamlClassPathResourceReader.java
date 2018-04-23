package org.obiba.mica.core.support;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

public class YamlClassPathResourceReader {

  private static ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
  }

  public static <T> T read(String resourcePath, Class<T> resultClass) {
    YamlMapFactoryBean factory = new YamlMapFactoryBean();
    factory.setResources(new ClassPathResource(resourcePath));

    return mapper.convertValue(factory.getObject(), resultClass);
  }
}
