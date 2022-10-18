package org.obiba.mica.search.reports.generators;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Sets;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.search.reports.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DatasetVariableCsvReportGenerator extends DocumentCsvReportGenerator {

  private static final Logger log = LoggerFactory.getLogger(DatasetVariableCsvReportGenerator.class);

  private final List<DatasetVariable> variables;

  private List<String> headers = new ArrayList<>();

  public DatasetVariableCsvReportGenerator(List<DatasetVariable> variables, String locale) {
    super(locale);
    this.variables = variables;
    initialize();
  }

  private void initialize() {
    headers.add("id");
    headers.add("studyId");
    headers.add("populationId");
    headers.add("dceId");
    headers.add("datasetId");
    headers.add("variableType");
    headers.add("name");
    headers.add("entityType");
    headers.add("referencedEntityType");
    headers.add("valueType");
    headers.add("repeatable");
    headers.add("occurrenceGroup");
    headers.add("unit");
    headers.add("mimeType");
    headers.add("nature");
    variables.stream()
      .filter(DatasetVariable::hasAttributes)
      .map(DatasetVariable::getAttributes)
      .forEach(attrs -> attributeKeys.addAll(attrs.keySet()));
    Set<String> attrHeaders = Sets.newTreeSet();
    attributeKeys.stream().map(key -> key.replace("__", "::")).forEach(attrHeaders::add);
    headers.addAll(attrHeaders);
  }

  @Override
  public void write(OutputStream outputStream) {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
      zipOutputStream.putNextEntry(new ZipEntry("variables.csv"));
      super.write(zipOutputStream);
      zipOutputStream.putNextEntry(new ZipEntry("categories.csv"));
      ReportGenerator reporter = new DatasetCategoryCsvReportGenerator(variables, getLocale());
      reporter.write(zipOutputStream);
    } catch (IOException e) {
      log.error("Error when reporting variables", e);
    }
  }

  @Override
  protected void writeHeader(CSVWriter writer) {
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  @Override
  protected void writeEachLine(CSVWriter writer) {
    variables.forEach(variable -> {
      List<String> line = new ArrayList<>();
      line.add(variable.getId());
      line.add(variable.getStudyId());
      line.add(variable.getPopulationId());
      line.add(variable.getDceId());
      line.add(variable.getDatasetId());
      line.add(variable.getVariableType().toString());
      line.add(variable.getName());
      line.add(variable.getEntityType());
      line.add(variable.getReferencedEntityType());
      line.add(variable.getValueType());
      line.add("" + variable.isRepeatable());
      line.add(variable.getOccurrenceGroup());
      line.add(variable.getUnit());
      line.add(variable.getMimeType());
      line.add(variable.getNature());

      Attributes attrs = variable.getAttributes();
      attributeKeys.forEach(key -> {
        if (attrs != null && attrs.containsKey(key)) {
          line.add(translate(attrs.get(key)));
        } else {
          line.add(null);
        }
      });

      writer.writeNext(line.toArray(new String[line.size()]));
    });
  }

}
