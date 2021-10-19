package org.obiba.mica.search.csvexport.generators;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Sets;
import jersey.repackaged.com.google.common.collect.Lists;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.dataset.domain.DatasetCategory;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

class DatasetCategoryCsvReportGenerator extends DocumentCsvReportGenerator {

  private static final Logger log = LoggerFactory.getLogger(DatasetCategoryCsvReportGenerator.class);

  private final List<DatasetVariable> variables;

  private List<String> headers = Lists.newArrayList();

  public DatasetCategoryCsvReportGenerator(List<DatasetVariable> variables, String locale) {
    super(locale);
    this.variables = variables;
    initialize();
  }

  private void initialize() {
    headers.add("variableId");
    headers.add("name");
    headers.add("missing");
    variables.stream()
      .filter(DatasetVariable::hasCategories)
      .forEach(variable -> variable.getCategories().stream()
        .filter(DatasetCategory::hasAttributes)
        .map(DatasetCategory::getAttributes)
        .forEach(attrs -> attributeKeys.addAll(attrs.keySet())));
    Set<String> attrHeaders = Sets.newTreeSet();
    attributeKeys.stream().map(key -> key.replace("__", "::")).forEach(attrHeaders::add);
    headers.addAll(attrHeaders);
  }

  @Override
  protected void writeHeader(CSVWriter writer) {
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  @Override
  protected void writeEachLine(CSVWriter writer) {
    variables.stream()
      .filter(DatasetVariable::hasCategories)
      .forEach(variable -> variable.getCategories().forEach(category -> {
        List<String> line = Lists.newArrayList();
        line.add(variable.getId());
        line.add(category.getName());
        line.add(category.isMissing() + "");

        Attributes attrs = category.getAttributes();
        attributeKeys.forEach(key -> {
          if (attrs != null && attrs.containsKey(key)) {
            line.add(translate(attrs.get(key)));
          } else {
            line.add(null);
          }
        });

        writer.writeNext(line.toArray(new String[line.size()]));
      }));
  }

}
