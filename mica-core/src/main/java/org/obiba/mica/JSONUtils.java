/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

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
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
    try {
      return mapper.writeValueAsString(map);
    } catch(JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Convert a JSON string to {@link Properties}.
   *
   * @param jsonStr
   * @return
   */
  public static Properties toProperties(String jsonStr) {
    return toProperties(toMap(jsonStr));
  }

  /**
   * Convert a (JSON) map to {@link Properties}.
   *
   * @param map
   * @return
   */
  public static Properties toProperties(Map<String, Object> map) {
    Properties properties = new Properties();
    appendProperties(properties, "", map);
    return properties;
  }

  private static void appendProperties(Properties properties, String path, Map<String, Object> map) {
    map.forEach((key, value) -> {
      String currentPath = Strings.isNullOrEmpty(path) ? key : path + "." + key;
      if (value instanceof Map) {
        appendProperties(properties, currentPath, (Map<String, Object>)value);
      } else {
        properties.put(currentPath, value.toString());
      }
    });
  }
}
