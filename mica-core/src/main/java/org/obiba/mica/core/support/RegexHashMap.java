package org.obiba.mica.core.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Object that matches regex patterns to values.
 */
public class RegexHashMap implements Map<String, Object> {

  private class PatterMatcher {
    private final String regex;
    private final Pattern compiled;

    PatterMatcher(String regex) {
      this.regex = regex;
      this.compiled = Pattern.compile(regex);
    }

    boolean matched(String string) {
      boolean matches = compiled.matcher(string).matches();
      if (matches) {
        inputToPatternMap.put(string, regex);
      }

      return matches;
    }
  }

  private final Map<String, String> inputToPatternMap;
  private final Map<String, Object> patternToValueMap;

  private final List<PatterMatcher> matchers;

  public RegexHashMap() {
    inputToPatternMap = new HashMap<>();
    patternToValueMap = new HashMap<>();
    matchers = new ArrayList<>();
  }

  @Override
  public int size() {
    return patternToValueMap.size();
  }

  @Override
  public boolean isEmpty() {
    return patternToValueMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return patternToValueMap.containsKey(key);
  }

  public boolean containsKeyPattern(Object key) {
    return inputToPatternMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return patternToValueMap.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    if (!inputToPatternMap.containsKey(key)) {
      for (PatterMatcher matcher : matchers) {
        if (key != null && matcher.matched(key.toString())) {
          break;
        }
      }
    }

    if (inputToPatternMap.containsKey(key)) {
      return patternToValueMap.get(inputToPatternMap.get(key));
    }

    return null;
  }

  @Override
  public Object put(String key, Object value) {
    Object previousValue = patternToValueMap.put(key, value);

    if (previousValue == null) {
      matchers.add(new PatterMatcher(key));
    }

    return previousValue;
  }

  @Override
  public Object remove(Object key) {
    Object removedValue = patternToValueMap.remove(key);

    if (removedValue != null) {
      for (Iterator<PatterMatcher> iterator = matchers.iterator(); iterator.hasNext();) {
        PatterMatcher matcher = iterator.next();
        if (matcher.regex.equals(key)) {
          iterator.remove();

          break;
        }
      }

      for (Iterator<Entry<String, String>> iterator = inputToPatternMap.entrySet().iterator(); iterator.hasNext();) {
        Entry<String, String> entry = iterator.next();
        if (entry.getValue().equals(key)) {
          iterator.remove();
        }
      }
    }

    return removedValue;
  }

  @Override
  public void putAll(Map<? extends String, ? extends Object> m) {
    m.forEach((k, v) -> put(k, v));
  }

  @Override
  public void clear() {
    inputToPatternMap.clear();
    patternToValueMap.clear();
    matchers.clear();
  }

  @Override
  public Set<String> keySet() {
    return patternToValueMap.keySet();
  }

  public Set<String> keySetPattern() {
    return inputToPatternMap.keySet();
  }

  @Override
  public Collection<Object> values() {
    return patternToValueMap.values();
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return patternToValueMap.entrySet();
  }

}