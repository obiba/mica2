package org.obiba.mica.study.search.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.yaml.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
public class AggregationYamlParser {

  private static final Logger log = LoggerFactory.getLogger(AggregationYamlParser.class);

  private static final String TYPE = ".type";

  private static final String NAME = ".name";

  public Iterable<AggregationBuilder<?>> getAggregations(String file) throws IOException {
    Collection<AggregationBuilder<?>> termsBuilders = new ArrayList<>();

    YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
    yamlPropertiesFactoryBean.setResources(new Resource[] { new ClassPathResource(file) });
    Properties properties = yamlPropertiesFactoryBean.getObject();
    for(Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();
      if(!key.contains(NAME) && !key.contains(TYPE)) {
        String name = getName(value, key);
        termsBuilders.add(AggregationBuilders.terms(name).field(key));
        log.debug("Add default terms aggregation: name={}, field={}", name, key);
      } else {
        if(key.contains(TYPE)) {
          parseSpecificAggregation(properties, key, value);
        }
      }
    }
    return termsBuilders;
  }

  private AggregationBuilder<?> parseSpecificAggregation(Properties properties, String key, String value) {
    String field = key.replace(TYPE, "");
    String name = getName(properties.getProperty(field + NAME), field);
    switch(value) {
      case "range":
        log.debug("Add range aggregation: name={}, field={}", name, field);
        return AggregationBuilders.range(name).field(field);
      default:
        throw new IllegalArgumentException("Unsupported aggregation type " + value + " for field " + field);
    }
  }

  private String getName(String name, String field) {
    return (Strings.isNullOrEmpty(name) ? field : name).replaceAll("\\.", "-");
  }

}
