package org.obiba.mica.search.aggregations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

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

  private static final String ALIAS = PROPERTIES + FIELD_SEPARATOR + "alias";

  private static final String LOCALIZED = PROPERTIES + FIELD_SEPARATOR + "localized";

  private static final String AGG_TERMS = "terms";

  private static final String AGG_STATS = "stats";

  private List<Locale> locales;

  private long minDocCount = 0;

  private Map<String, Properties> yamlPropertiesCache = new ConcurrentHashMap<>();

  public void setLocales(List<Locale> locales) {
    this.locales = locales;
  }

  /**
   * Makes Bucket aggregataions to return at least the given number of documents. Zero returns empty buckets as well.
   * A value of -1 will exclude this property
   * @param value
   */
  public void setMinDocCount(long value) {
    minDocCount = value;
  }

  public Iterable<AbstractAggregationBuilder> getAggregations(@Nullable Resource description,
    @Nullable Map<String, Properties> subProperties) throws IOException {
    if(description == null) return Collections.emptyList();

    String descriptionUri = description.getURI().toString();

    if (!yamlPropertiesCache.containsKey(descriptionUri)) {
      YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
      yamlPropertiesFactoryBean.setResources(new Resource[] { description });
      yamlPropertiesCache.put(descriptionUri.toString(), yamlPropertiesFactoryBean.getObject());
    }

    return getAggregations(yamlPropertiesCache.get(descriptionUri), subProperties);
  }

  public Iterable<AbstractAggregationBuilder> getAggregations(@Nullable Properties properties) throws IOException {
    return getAggregations(properties, null);
  }

  public Iterable<AbstractAggregationBuilder> getAggregations(@Nullable Properties properties,
    @Nullable Map<String, Properties> subProperties) throws IOException {
    if(properties == null) return Collections.emptyList();

    Map<String, Iterable<AbstractAggregationBuilder>> subAggregations = Maps.newHashMap();
    if(subProperties != null) {
      subProperties.forEach((key, subs) -> subAggregations.put(key, parseAggregations(subs, null)));
    }

    return parseAggregations(properties, subAggregations);
  }

  private Iterable<AbstractAggregationBuilder> parseAggregations(@Nullable Properties properties,
    Map<String, Iterable<AbstractAggregationBuilder>> subAggregations) {
    Collection<AbstractAggregationBuilder> termsBuilders = new ArrayList<>();
    if (properties == null) return termsBuilders;

    SortedMap<String, ?> sortedSystemProperties = new TreeMap(properties);
    String prevKey = null;
    for(Map.Entry<String, ?> entry : sortedSystemProperties.entrySet()) {
      String key = entry.getKey().replaceAll("\\" + PROPERTIES + ".*$", "");
      if(!key.equals(prevKey)) {
        parseAggregation(termsBuilders, properties, key, subAggregations);
        prevKey = key;
      }
    }

    return termsBuilders;
  }

  private void parseAggregation(Collection<AbstractAggregationBuilder> termsBuilders, Properties properties,
    String key, Map<String, Iterable<AbstractAggregationBuilder>> subAggregations) {
    Boolean localized = Boolean.valueOf(properties.getProperty(key + LOCALIZED));
    String alias = properties.getProperty(key + ALIAS);
    String type = getAggregationType(properties.getProperty(key + TYPE), localized);
    createAggregation(termsBuilders, getFields(key, alias, localized), type, subAggregations);
  }

  private void createAggregation(Collection<AbstractAggregationBuilder> termsBuilders, Map<String, String> fields,
    String type, Map<String, Iterable<AbstractAggregationBuilder>> subAggregations) {
    fields.entrySet().forEach(entry -> {
      log.info("Building aggregation '{}' of type '{}'", entry.getKey(), type);

      switch(type) {
        case AGG_TERMS:
          TermsBuilder termBuilder = AggregationBuilders.terms(entry.getKey()).field(entry.getValue());
          if (minDocCount > -1) termBuilder.minDocCount(minDocCount);
          if (subAggregations != null && subAggregations.containsKey(entry.getKey())) {
            subAggregations.get(entry.getKey()).forEach(termBuilder::subAggregation);
          }
          termsBuilders.add(termBuilder.order(Terms.Order.term(true)).size(0));
          break;
        case AGG_STATS:
          termsBuilders.add(AggregationBuilders.stats(entry.getKey()).field(entry.getValue()));
          break;
        default:
          throw new IllegalArgumentException("Invalid aggregation type detected: " + type);
      }
    });
  }

  private Map<String, String> getFields(String field, String alias, Boolean localized) {
    String name = formatName(Strings.isNullOrEmpty(alias) ? field : alias);
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
   *
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
