/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.harmonized;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.web.model.Mica;

import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVWriter;
import sun.util.locale.LanguageTag;

import static org.obiba.mica.web.model.Mica.DatasetVariablesHarmonizationsDto;

public class CsvHarmonizationVariablesWriter {

  private final List<String> namespaces;

  public CsvHarmonizationVariablesWriter(List<String> namespaces) {
    this.namespaces = namespaces;
  }

  public ByteArrayOutputStream write(HarmonizationDataset dataset,
      DatasetVariablesHarmonizationsDto harmonizationVariables, String locale) throws IOException {

    ByteArrayOutputStream values = new ByteArrayOutputStream();
    CSVWriter writer = null;
    try {
      writer = new CSVWriter(new PrintWriter(values));
      writeHeader(writer, dataset, locale);
      writeBody(writer, dataset, harmonizationVariables, locale);
    } finally {
      if(writer != null) writer.close();
    }

    return values;
  }

  private void writeHeader(CSVWriter writer, HarmonizationDataset dataset, String locale) {
    List<String> headers = dataset.getStudyTables().stream()
        .map(table -> {
          LocalizedString name = table.getName();
          String id = table.getStudyId();
          return id + (name != null ?  " (" + name.get(locale) +")" : "");
        })
        .collect(Collectors.toList());

    headers.add(0, "");
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  private void writeBody(CSVWriter writer, HarmonizationDataset dataset,
      DatasetVariablesHarmonizationsDto harmonizationVariables, String locale) {

    harmonizationVariables.getVariableHarmonizationsList().forEach(
        variableHarmonization -> {
          List<String> row = Lists.newArrayList();
          dataset.getStudyTables().forEach(
            studyTable -> {
              variableHarmonization.getDatasetVariableSummariesList().forEach(
                summary -> {
                  Mica.DatasetVariableResolverDto resolver = summary.getResolver();
                  if (resolver.getStudyId().equals(studyTable.getStudyId())
                      && resolver.getProject().equals(studyTable.getProject())
                      && resolver.getTable().equals(studyTable.getTable())) {

                    row.add(getStatus(summary, locale));
                    return;
                  }
                });

              if (row.size() == 0) {
                row.add("-");
              }
            }
          );

          row.add(0, variableHarmonization.getResolver().getName());
          writer.writeNext(row.toArray(new String[row.size()]));
        }
    );
  }

  private String getStatus(Mica.DatasetVariableSummaryDto summary, String locale) {

    Optional<Mica.LocalizedStringDto> result = summary.getAttributesList().stream()
        .filter(attribute -> namespaces.contains(attribute.getNamespace()) && attribute.getName().equals("status"))
        .map(Mica.AttributeDto::getValuesList).flatMap((values) -> values.stream()).filter(value -> {
          String lang = value.getLang();
          return locale.equals(lang) || LanguageTag.UNDETERMINED.equals(lang);
        }).findFirst();


    return result.isPresent() ? result.get().getValue()  : "-";
  }

}
