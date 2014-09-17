package org.obiba.mica.search.rest;

import com.google.common.base.Strings;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.yaml.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class AggregationYamlParser {

  private static final Logger log = LoggerFactory.getLogger(AggregationYamlParser.class);

  private static final String FIELD_SEPARATOR = ".";

  private static final String NAME_SEPARATOR = "-";

  private static final String UND_LOCALE = Locale.forLanguageTag("und").toLanguageTag();

  private static final String UND_LOCALE_FIELD = FIELD_SEPARATOR + UND_LOCALE;

  private static final String UND_LOCALE_NAME = NAME_SEPARATOR + UND_LOCALE;

  private static final String DEFAULT_LOCALE = Locale.ENGLISH.getLanguage();

  private static final String DEFAULT_LOCALE_FIELD = FIELD_SEPARATOR + DEFAULT_LOCALE;

  private static final String DEFAULT_LOCALE_NAME = NAME_SEPARATOR + DEFAULT_LOCALE;

  private static final String PROPERTIES = FIELD_SEPARATOR + "properties";

  private static final String TYPE = PROPERTIES + FIELD_SEPARATOR + "type";

  private static final String LOCALIZED = PROPERTIES + FIELD_SEPARATOR + "localized";

  private static final String AGG_TERMS = "terms";

  private static final String AGG_STATS = "stats";

  private List<Locale> locales;

  public Iterable<AbstractAggregationBuilder> getAggregations(String file) throws IOException {
    return getAggregations(new ClassPathResource(file));
  }

  public void setLocales(List<Locale> locales) {
    this.locales = locales;
  }

  public Iterable<AbstractAggregationBuilder> getAggregations(Resource description) throws IOException {
    YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
    yamlPropertiesFactoryBean.setResources(new Resource[] { description });
    return getAggregations(yamlPropertiesFactoryBean.getObject());
  }

  public Iterable<AbstractAggregationBuilder> getAggregations(Properties properties) throws IOException {
    Collection<AbstractAggregationBuilder> termsBuilders = new ArrayList<>();
    SortedMap<String, ?> sortedSystemProperties = new TreeMap(properties);
    String prevKey = null;
    for(Map.Entry<String, ?> entry : sortedSystemProperties.entrySet()) {
      String key = entry.getKey().replaceAll("\\" + PROPERTIES + ".*$", "");
      if(!key.equals(prevKey)) {
        parseAggregation(termsBuilders, properties, key);
        prevKey = key;
      }
    }

    return termsBuilders;
  }

  private void parseAggregation(Collection<AbstractAggregationBuilder> termsBuilders, Properties properties,
      String key) {
    Boolean localized = Boolean.valueOf(properties.getProperty(key + LOCALIZED));
    String type = getAggregationType(properties.getProperty(key + TYPE), localized);
    createAggregation(termsBuilders, getFields(key, localized), type);
  }

  private void createAggregation(Collection<AbstractAggregationBuilder> termsBuilders, Map<String, String> fields,
      String type) {
    fields.entrySet().forEach(entry -> {
      log.info("Building aggregation '{}' of type '{}'", entry.getKey(), type);

      switch(type) {
        case AGG_TERMS:
          termsBuilders
              .add(AggregationBuilders.terms(entry.getKey()).field(entry.getValue()).order(Terms.Order.term(true)));
          break;
        case AGG_STATS:
          termsBuilders.add(AggregationBuilders.stats(entry.getKey()).field(entry.getValue()));
          break;
        default:
          throw new IllegalArgumentException("Invalid aggregation type detected: " + type);
      }
    });
  }

  private Map<String, String> getFields(String field, Boolean localized) {
    String name = formatName(field);
    final Map<String, String> fields = new HashMap<>();
    if(localized) {
      fields.put(name + UND_LOCALE_NAME, field + UND_LOCALE_FIELD);

      if(locales != null) {
        locales.stream()
            .forEach(locale -> fields.put(name + NAME_SEPARATOR + locale, field + FIELD_SEPARATOR + locale));
      } else {
        fields.put(name + DEFAULT_LOCALE_NAME, field + DEFAULT_LOCALE_FIELD);
      }
    } else {
      fields.put(name, field);
    }

    return fields;
  }

  /**
   * Default the type to 'terms' if localized is true, otherwise use valid input type
   * @param type
   * @param localized
   * @return
   */
  private String getAggregationType(String type, Boolean localized) {
    return !localized && !Strings.isNullOrEmpty(type) && type.matches(String.format("^(%s|%s)$", AGG_STATS, AGG_TERMS))
        ? type
        : AGG_TERMS;
  }

  public static String formatName(String name) {
    return name.replaceAll("\\" + FIELD_SEPARATOR, NAME_SEPARATOR);
  }

  public static String unformatName(String name) {
    return name.replaceAll(NAME_SEPARATOR, FIELD_SEPARATOR);
  }
}
