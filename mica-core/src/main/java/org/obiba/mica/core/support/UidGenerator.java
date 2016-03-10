package org.obiba.mica.core.support;

import java.util.Collection;
import java.util.StringJoiner;

import com.google.common.base.Joiner;

public class UidGenerator {

  public static final String SEPARATOR = ":";

  public static String getUId(Collection<String> parts) {
    return getUId(parts, SEPARATOR);
  }

  public static String getUId(Collection<String> parts, String separtor) {
    return Joiner.on(separtor).join(parts);
  }
}
