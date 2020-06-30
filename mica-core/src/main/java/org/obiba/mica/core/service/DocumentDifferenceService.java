package org.obiba.mica.core.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.MapDifference.ValueDifference;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import org.obiba.mica.core.support.RegexHashMap;

import net.minidev.json.JSONArray;

public class DocumentDifferenceService {

  private static final ObjectMapper mapper = new ObjectMapper();

  private DocumentDifferenceService() { }

  public static MapDifference<String, Object> diff(Object left, Object right, RegexHashMap translationMap) throws JsonProcessingException {
    if (translationMap == null || translationMap.size() == 0) translationMap = new RegexHashMap();

    return Maps.difference(flatten(left, translationMap), flatten(right, translationMap));
  }

  public static MapDifference<String, Object> diff(Object left, Object right) throws JsonProcessingException {
    return diff(left, right, new RegexHashMap());
  }

  public static Map<String, Object> flatten(Object object, RegexHashMap translationMap) throws JsonProcessingException {
    Map<String, Object> map = new RegexHashMap();
    if (object == null) return map;

    final String string = mapper.writeValueAsString(object);

    JSONArray array = JsonPath.using(Configuration.builder().options(Option.AS_PATH_LIST).build()).parse(string).read("$..*");

    array.stream().map(Object::toString)
    .filter(key -> !key.startsWith("$['logo']") && !key.startsWith("$['createdDate']") && !key.startsWith("$['lastModifiedDate']") && !key.startsWith("$['membershipSortOrder']"))
    .forEach(key -> {
      Object read = JsonPath.parse(string).read(key);
      if (read != null && !(read instanceof Map) && !(read instanceof List)) {
        String processedKey = key.replaceAll("(\\$\\[')|('\\])", "").replaceAll("(\\[')", ".");
        
        Object translatedKey = translationMap.get(processedKey);

        map.put(translatedKey != null ? translatedKey.toString() : processedKey, read);
      }
    });

    return map;
  }

  public static Map<String, Object> flatten(Object object) throws JsonProcessingException {
    return flatten(object, new RegexHashMap());
  }

  public static Map<String, List<Object>> fromEntriesDifferenceMap(Map<String, ValueDifference<Object>> entriesDiffering) {
    Map<String, List<Object>> result = new HashMap<>();

    entriesDiffering.forEach((key, valueDifference) -> result.put(key, Arrays.asList(valueDifference.leftValue(), valueDifference.rightValue())));

    return result;
  }
}