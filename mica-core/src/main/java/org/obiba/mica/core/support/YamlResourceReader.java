package org.obiba.mica.core.support;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileSystemResource;

public class YamlResourceReader {

  private static ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
  }

  public static <T> T readClassPath(String resourcePath, Class<T> resultClass) {
    YamlMapFactoryBean factory = new YamlMapFactoryBean();
    factory.setResources(new ClassPathResource(resourcePath));

    return mapper.convertValue(factory.getObject(), resultClass);
  }

  public static <T> T readFile(String resourcePath, Class<T> resultClass) {
    YamlMapFactoryBean factory = new YamlMapFactoryBean();
    factory.setResources(new FileSystemResource(resourcePath));

    return mapper.convertValue(factory.getObject(), resultClass);
  }
}
