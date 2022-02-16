package org.obiba.mica.search.reports.generators;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.obiba.mica.core.domain.AbstractAttributeModelAware;
import org.obiba.mica.core.domain.AbstractModelAware;
import org.obiba.mica.core.domain.LocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class DocumentCsvReportGenerator extends CsvReportGenerator {

  private static final Logger log = LoggerFactory.getLogger(DocumentCsvReportGenerator.class);

  private final String locale;

  private Set<String> modelKeys = Sets.newTreeSet();

  protected Set<String> attributeKeys = Sets.newTreeSet();

  private Map<String, Map<String, Object>> models = Maps.newHashMap();

  protected DocumentCsvReportGenerator(String locale) {
    this.locale = locale;
  }

  @Override
  public void write(OutputStream outputStream, boolean omitHeader) {
    try {
      CSVWriter writer = new CSVWriter(new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8")));
      if (!omitHeader) writeHeader(writer);
      writeEachLine(writer);
      writer.flush();
      outputStream.flush();
    } catch (IOException e) {
      log.error("CSV report extraction failed", e);
      throw new UncheckedIOException(e);
    } catch (Exception e) {
      log.error("CSV report extraction failed", e);
    }
  }

  public String getLocale() {
    return locale;
  }

  public Map<String, Map<String, Object>> getModels() {
    return models;
  }

  public Set<String> getModelKeys() {
    return modelKeys;
  }

  protected void appendModel(String id, AbstractAttributeModelAware modelAware) {
    Map<String, Object> flatModel = flatten(modelAware.getModel());
    models.put(id, flatModel);
    modelKeys.addAll(flatModel.keySet());
  }

  protected void appendModel(AbstractModelAware modelAware) {
    Map<String, Object> flatModel = flatten(modelAware.getModel());
    models.put(modelAware.getId(), flatModel);
    modelKeys.addAll(flatModel.keySet());
  }

  protected String translate(Object stringObj) {
    if (stringObj == null) return "";
    if (stringObj instanceof LocalizedString) {
      String value = ((LocalizedString) stringObj).get(locale);
      if (Strings.isNullOrEmpty(value))
        return ((LocalizedString) stringObj).getUndetermined();
      return value;
    }
    if (stringObj instanceof Map && ((Map) stringObj).containsKey(locale))
      return ((Map) stringObj).get(locale).toString();
    return stringObj.toString();
  }

  protected Map<String, Object> flatten(Map<String, Object> model) {
    Map<String, Object> map = Maps.newHashMap();
    for (Map.Entry<String, Object> entry : model.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof List) {
        List list = (List) value;
        if (list.isEmpty()) {
          map.put(entry.getKey(), "");
        } else {
          Object firstValue = list.get(0);
          if (firstValue instanceof String || firstValue instanceof Number || firstValue instanceof Boolean) {
            map.put(entry.getKey(), Joiner.on("|").join(list));
          } else if (firstValue instanceof LocalizedString || (firstValue instanceof Map && ((Map<?, ?>) firstValue).containsKey(locale))) {
            List<String> trList = (List<String>) list.stream().map(ls -> translate(ls)).collect(Collectors.toList());
            map.put(entry.getKey(), Joiner.on("|").join(trList));
          } else {
            for (int i = 0; i < list.size(); i++) {
              map.put(String.format("%s[%s]", entry.getKey(), i), list.get(i) == null ? "" : list.get(i).toString());
            }
          }
        }
      } else if (value instanceof LocalizedString) {
        map.put(entry.getKey(), translate(entry.getValue()));
      } else if (value instanceof Map) {
        Map<String, Object> valueMap = (Map<String, Object>) entry.getValue();
        if (valueMap.containsKey(locale))
          map.put(entry.getKey(), valueMap.get(locale).toString());
        else
          flatten(valueMap).forEach((key, val) -> map.put(entry.getKey() + "." + key, val));
      } else {
        map.put(entry.getKey(), value);
      }
    }
    return map;
  }

}
