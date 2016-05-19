package org.obiba.mica;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONUtils {

  /**
   * Convert JSON string to map.
   *
   * @param jsonStr
   * @return
   */
  public static Map<String, Object> toMap(String jsonStr) {
    ObjectMapper mapper = new ObjectMapper();
    // convert JSON string to Map
    try {
      return mapper.readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Convert a map to a JSON string.
   *
   * @param map
   * @return
   */
  public static String toJSON(Map<String, Object> map) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(map);
    } catch(JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

}
