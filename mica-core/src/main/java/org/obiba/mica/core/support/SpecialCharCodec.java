package org.obiba.mica.core.support;

import java.util.Map;
import java.util.stream.Collectors;

public class SpecialCharCodec {

  private final Map<String, String> encodeMap;
  private final Map<String, String> decodeMap;

  public SpecialCharCodec(Map<String, String> encodeMap) {
    this.encodeMap = encodeMap;
    this.decodeMap = encodeMap.entrySet().stream()
      .collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));
  }

  public String encode(String input) {
    if (input == null) return null;
    String result = input;
    for (Map.Entry<String, String> entry : encodeMap.entrySet()) {
      result = result.replace(entry.getKey(), entry.getValue());
    }
    return result;
  }

  public String decode(String input) {
    if (input == null) return null;
    String result = input;
    for (Map.Entry<String, String> entry : decodeMap.entrySet()) {
      result = result.replace(entry.getKey(), entry.getValue());
    }
    return result;
  }

  public Map<String, String> getEncodeMap() {
    return encodeMap;
  }

  public Map<String, String> getDecodeMap() {
    return decodeMap;
  }
}
