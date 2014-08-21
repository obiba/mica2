package org.obiba.mica.search.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
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

  private static final String TYPE = ".agg.type";

  private static final String FIELD = ".agg.field";

  public Iterable<AbstractAggregationBuilder> getAggregations(String file) throws IOException {
    return getAggregations(new ClassPathResource(file));
  }

  public Iterable<AbstractAggregationBuilder> getAggregations(Resource description) throws IOException {
    Collection<AbstractAggregationBuilder> termsBuilders = new ArrayList<>();

    YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
    yamlPropertiesFactoryBean.setResources(new Resource[] { description });
    Properties properties = yamlPropertiesFactoryBean.getObject();
    for(Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();
      if(key.contains(TYPE)) {
        termsBuilders.add(parseSpecificAggregation(properties, key, value));
        log.debug("Add specific aggregation: name={}, field={}", key, value);
      } else if (!key.contains(FIELD)) {
        String name = getName(value, key);
        termsBuilders.add(AggregationBuilders.terms(name).field(key).order(Terms.Order.term(true)));
        log.debug("Add default terms aggregation: name={}, field={}", name, key);
      }
    }

    return termsBuilders;
  }

  private AbstractAggregationBuilder parseSpecificAggregation(Properties properties, String key, String value) {
    String prefix = key.replace(TYPE, "");
    String field =  prefix + "." + properties.getProperty(prefix + FIELD);
    String name = getName(field, "");

    switch(value) {
      case "stats":
        log.debug("Add stats aggregation: name={}, field={}", name, field);
        return AggregationBuilders.stats(name).field(field);
      case "range":
        log.debug("Add range aggregation: name={}, field={}", name, field);
        // TODO remove this method if no range can be provided! Added bound to prevent possible crash
        return AggregationBuilders.range(name).field(field).addUnboundedTo(Integer.MAX_VALUE);
      default:
        throw new IllegalArgumentException("Unsupported aggregation type " + value + " for field " + field);
    }
  }

  private String getName(String name, String field) {
    return (Strings.isNullOrEmpty(name) ? field : name).replaceAll("\\.", "-");
  }

}
