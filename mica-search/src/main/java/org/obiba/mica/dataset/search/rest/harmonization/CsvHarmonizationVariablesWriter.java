/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.harmonization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.obiba.mica.core.domain.HarmonizationStudyTable;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.web.model.Mica;

import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVWriter;

import static org.obiba.mica.web.model.Mica.DatasetVariablesHarmonizationsDto;

public class CsvHarmonizationVariablesWriter {

  private final List<String> namespaces;

  private static final String LANGUAGE_TAG_UNDETERMINED = "und";

  private static final String EMPTY = "";

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
    List<String> headers = Lists.newArrayList(dataset.getBaseStudyTables()).stream()
        .map(table -> {
          LocalizedString name = table.getName();
          String id = table instanceof StudyTable ? ((StudyTable)table).getStudyId() : ((HarmonizationStudyTable) table).getStudyId();

          return id + (name != null ?  " (" + name.get(locale) +")" : "");
        })
        .collect(Collectors.toList());

    headers.add(0, "Variable");
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  private void writeBody(CSVWriter writer, HarmonizationDataset dataset,
      DatasetVariablesHarmonizationsDto harmonizationVariables, String locale) {

    harmonizationVariables.getVariableHarmonizationsList().forEach(
        variableHarmonization -> {
          List<String> row = Lists.newArrayList();
          dataset.getBaseStudyTables().forEach(
            table -> {
              final boolean[] found = { false };
              variableHarmonization.getDatasetVariableSummariesList().forEach(
                summary -> {
                  String id = table.getStudyId();
                  Mica.DatasetVariableResolverDto resolver = summary.getResolver();
                  if ((resolver.getStudyId().equals(id)
                      && resolver.getSource().equals(table.getSource()))) {

                    String statusDetail = getAttributeByName(summary, "status_detail", locale);

                    row.add(getStatus(summary, locale) + (EMPTY.equals(statusDetail) ? "" : " (" + getAttributeByName(summary, "status_detail", locale) + ")"));
                    found[0] = true;
                    return;
                  }
                });

              if (row.size() == 0 || !found[0]) {
                row.add(EMPTY);
              }
            }
          );

          row.add(0, variableHarmonization.getResolver().getName());
          writer.writeNext(row.toArray(new String[row.size()]));
        }
    );
  }

  private String getStatus(Mica.DatasetVariableSummaryDto summary, String locale) {
    return getAttributeByName(summary, "status", locale);
  }

  private String getAttributeByName(Mica.DatasetVariableSummaryDto summary, String attributeName, String locale) {
    Optional<Mica.LocalizedStringDto> result = summary.getAttributesList().stream()
        .filter(attribute -> namespaces.contains(attribute.getNamespace()) && attribute.getName().equals(attributeName))
        .map(Mica.AttributeDto::getValuesList).flatMap(Collection::stream).filter(value -> {
          String lang = value.getLang();
          return locale.equals(lang) || LANGUAGE_TAG_UNDETERMINED.equals(lang);
        }).findFirst();


    return result.isPresent() ? result.get().getValue() : EMPTY;
  }

}
