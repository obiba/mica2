package org.obiba.mica.core.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Map;

public class SpecialCharCodecFactory {

  private static final String DEFAULT_RESOURCE = "codec/special-char-map.json";
  private static SpecialCharCodec instance;

  public static SpecialCharCodec get() {
    if (instance == null) {
      instance = loadFromClasspath();
    }
    return instance;
  }

  private static SpecialCharCodec loadFromClasspath() {
    try (InputStream input = SpecialCharCodecFactory.class.getClassLoader().getResourceAsStream(SpecialCharCodecFactory.DEFAULT_RESOURCE)) {
      if (input == null) return new SpecialCharCodec(Map.of());

      ObjectMapper mapper = new ObjectMapper();
      Map<String, String> mappings = mapper.readValue(input, new TypeReference<>() {
      });
      return new SpecialCharCodec(mappings != null ? mappings : Map.of());
    } catch (Exception e) {
      return new SpecialCharCodec(Map.of());
    }
  }
}
