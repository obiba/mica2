/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.csvexport.generators;

import java.util.ArrayList;
import java.util.List;

import org.obiba.core.translator.Translator;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;

import au.com.bytecode.opencsv.CSVWriter;

public class VariablesCsvReportGenerator extends CsvReportGeneratorImpl {

  private static final String NOT_EXISTS = "-";

  private List<String> columnsToHide;
  private List<Mica.DatasetVariableResolverDto> datasetVariableDtos;
  private Translator translator;

  public VariablesCsvReportGenerator(MicaSearch.JoinQueryResultDto queryResult, List<String> columnsToHide, Translator translator) {
    this.columnsToHide = columnsToHide;
    this.datasetVariableDtos = queryResult.getVariableResultDto().getExtension(MicaSearch.DatasetVariableResultDto.result).getSummariesList();
    this.translator = translator;
  }

  @Override
  protected void writeHeader(CSVWriter writer) {

    List<String> line = new ArrayList<>();

    line.add("name");
    line.add("search.variable.label");
    if (mustShow("showVariablesTypeColumn"))
      line.add("type");
    if (mustShow("showVariablesStudiesColumn"))
      line.add("search.study.label");
    if (mustShow("showVariablesDatasetsColumn"))
      line.add("search.dataset.label");

    String[] translatedLine = line.stream().map(key -> translator.translate(key)).toArray(String[]::new);

    writer.writeNext(translatedLine);
  }

  @Override
  protected void writeEachLine(CSVWriter writer) {

    for (Mica.DatasetVariableResolverDto datasetVariableDto : datasetVariableDtos) {
      List<String> lineContent = generateLineContent(datasetVariableDto);
      writer.writeNext(lineContent.toArray(new String[lineContent.size()]));
    }
  }

  private List<String> generateLineContent(Mica.DatasetVariableResolverDto datasetVariableDto) {

    List<String> line = new ArrayList<>();

    line.add(datasetVariableDto.getName());
    line.add(datasetVariableDto.getVariableLabelCount()>0 ? datasetVariableDto.getVariableLabel(0).getValue() : "");
    if (mustShow("showVariablesTypeColumn"))
      line.add(translator.translate(
        String.format("variable_taxonomy.vocabulary.variableType.term.%s.title", datasetVariableDto.getVariableType())));

    if (mustShow("showVariablesStudiesColumn"))
      line.add(getStudyOrNetworkName(datasetVariableDto));
    if (mustShow("showVariablesDatasetsColumn"))
      line.add(datasetVariableDto.getDatasetAcronym(0).getValue());

    return line;
  }

  private String getStudyOrNetworkName(Mica.DatasetVariableResolverDto datasetVariableDto) {
    if (datasetVariableDto.getStudyAcronymCount() > 0)
      return datasetVariableDto.getStudyAcronym(0).getValue();
    else if (datasetVariableDto.getNetworkAcronymCount() > 0)
      return datasetVariableDto.getNetworkAcronym(0).getValue();
    else
      return NOT_EXISTS;
  }

  private boolean mustShow(String column) {
    return !columnsToHide.contains(column);
  }
}
