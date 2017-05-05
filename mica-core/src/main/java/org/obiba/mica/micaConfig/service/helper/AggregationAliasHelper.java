package org.obiba.mica.micaConfig.service.helper;

final public class AggregationAliasHelper {

  public static final String FIELD_SEPARATOR = ".";

  public static final String NAME_SEPARATOR = "-";

  public static String formatName(String name) {
    return name.replaceAll("\\" + FIELD_SEPARATOR, NAME_SEPARATOR);
  }

  public static String unformatName(String name) {
    return name.replaceAll(NAME_SEPARATOR, FIELD_SEPARATOR);
  }


}
