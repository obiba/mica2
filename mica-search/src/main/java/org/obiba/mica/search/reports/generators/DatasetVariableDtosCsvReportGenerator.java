/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.reports.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.obiba.core.translator.Translator;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;

import au.com.bytecode.opencsv.CSVWriter;

public class DatasetVariableDtosCsvReportGenerator extends CsvReportGenerator {

  private static final String NOT_EXISTS = "-";

  private List<String> columnsToHide;
  private List<Mica.DatasetVariableResolverDto> datasetVariableDtos;
  private Translator translator;

  public DatasetVariableDtosCsvReportGenerator(MicaSearch.JoinQueryResultDto queryResult, List<String> columnsToHide, Translator translator) {
    this.columnsToHide = columnsToHide;
    this.datasetVariableDtos = queryResult.getVariableResultDto().getExtension(MicaSearch.DatasetVariableResultDto.result).getSummariesList();
    this.translator = translator;
  }

  @Override
  protected void writeHeader(CSVWriter writer) {

    List<String> line = new ArrayList<>();

    line.add("name");
    line.add("search.variable.label");
    if(mustShow("showVariablesAnnotationsColumn"))
      line.add("client.label.variable.annotations");
    if (mustShow("showVariablesUnitColumn"))
      line.add("client.label.variable.unit");
    if (mustShow("showVariablesValueTypeColumn"))
      line.add("variable_taxonomy.vocabulary.valueType.title");
    if (mustShow("showVariablesCategoriesColumn"))
      line.add("client.label.variable.categories");
    if (mustShow("showVariablesTypeColumn"))
      line.add("type");
    if (mustShow("showVariablesStudiesColumn")) {
      line.add("search.study.label");
      line.add("search.study.population-name");
      line.add("search.study.dce-name");
    }
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
    if (mustShow("showVariablesAnnotationsColumn"))
      line.add(datasetVariableDto.getAnnotationsList().stream().map(annotationDto -> annotationDto.getTaxonomy() + "::" + annotationDto.getVocabulary() + "::" + annotationDto.getValue()).collect(Collectors.joining(" | ")));
    if (mustShow("showVariablesUnitColumn"))
      line.add(datasetVariableDto.getUnit());
    if (mustShow("showVariablesValueTypeColumn")) {
      if (datasetVariableDto.hasValueType())
      line.add(translator.translate(
        String.format("variable_taxonomy.vocabulary.valueType.term.%s.title", datasetVariableDto.getValueType())));
      else line.add("");
    }
    if (mustShow("showVariablesCategoriesColumn")) {
      if (datasetVariableDto.getCategoriesCount()>0)
        line.add(datasetVariableDto.getCategoriesList().stream().map(cat -> {
          String rval = cat.getName();
          Optional<Mica.AttributeDto> lblAttrOpt = cat.getAttributesList().stream().filter(attr -> attr.getName().equals("label")).findFirst();
          if (lblAttrOpt.isPresent() && !lblAttrOpt.get().getValuesList().isEmpty()) {
            rval = rval + ": " + lblAttrOpt.get().getValuesList().get(0).getValue();
          }
          return rval;
        }).collect(Collectors.joining(" | ")));
      else line.add("");
    }
    if (mustShow("showVariablesTypeColumn"))
      line.add(translator.translate(
        String.format("variable_taxonomy.vocabulary.variableType.term.%s.title", datasetVariableDto.getVariableType())));

    if (mustShow("showVariablesStudiesColumn")) {
      line.add(getStudyOrNetworkName(datasetVariableDto));

      if (datasetVariableDto.getPopulationNameCount() > 0) line.add(datasetVariableDto.getPopulationName(0).getValue());
      else line.add(datasetVariableDto.getPopulationId());

      if (datasetVariableDto.getDceNameCount() > 0) line.add(datasetVariableDto.getDceName(0).getValue());
      else line.add("");
    }
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
