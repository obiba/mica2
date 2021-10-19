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

public class DatasetDtosCsvReportGenerator extends CsvReportGeneratorImpl {

  private static final String NOT_EXISTS = "-";

  private List<String> columnsToHide;
  private List<Mica.DatasetDto> datasetDtos;
  private Translator translator;

  public DatasetDtosCsvReportGenerator(MicaSearch.JoinQueryResultDto queryResult, List<String> columnsToHide, Translator translator) {
    this.columnsToHide = columnsToHide;
    this.datasetDtos = queryResult.getDatasetResultDto().getExtension(MicaSearch.DatasetResultDto.result).getDatasetsList();
    this.translator = translator;
  }

  @Override
  protected void writeHeader(CSVWriter writer) {

    List<String> line = new ArrayList<>();

    if (mustShow("showDatasetsAcronymColumn"))
      line.add("acronym");
    line.add("name");
    if (mustShow("showDatasetsTypeColumn"))
      line.add("type");

    if (mustShow("showDatasetsNetworkColumn"))
      line.add("networks");
    if (mustShow("showDatasetsStudiesColumn"))
      line.add("studies");
    if (mustShow("showDatasetsVariablesColumn"))
      line.add("variables");

    String[] translatedLine = line.stream().map(key -> translator.translate(key)).toArray(String[]::new);

    writer.writeNext(translatedLine);
  }

  @Override
  protected void writeEachLine(CSVWriter writer) {

    for (Mica.DatasetDto datasetDto : datasetDtos) {
      List<String> lineContent = generateLineContent(datasetDto);
      writer.writeNext(lineContent.toArray(new String[lineContent.size()]));
    }
  }

  private List<String> generateLineContent(Mica.DatasetDto datasetDto) {

    List<String> line = new ArrayList<>();

    MicaSearch.CountStatsDto stats = datasetDto.getExtension(MicaSearch.CountStatsDto.datasetCountStats);

    if (mustShow("showDatasetsAcronymColumn"))
      line.add(datasetDto.getAcronym(0).getValue());
    line.add(datasetDto.getName(0).getValue());
    if (mustShow("showDatasetsTypeColumn"))
      line.add(findType(datasetDto));

    if (mustShow("showDatasetsNetworkColumn"))
      line.add(getNot0ValueOrDefault(stats.getNetworks()));
    if (mustShow("showDatasetsStudiesColumn"))
      line.add(getNot0ValueOrDefault(stats.getStudies()));
    if (mustShow("showDatasetsVariablesColumn"))
      line.add(getNot0ValueOrDefault(stats.getVariables()));

    return line;
  }

  private String findType(Mica.DatasetDto datasetDto) {
    if (datasetDto.hasExtension(Mica.HarmonizedDatasetDto.type))
      return translator.translate("dataset_taxonomy.vocabulary.className.term.HarmonizationDataset.title");
    else if (datasetDto.hasExtension(Mica.CollectedDatasetDto.type))
      return translator.translate("dataset_taxonomy.vocabulary.className.term.StudyDataset.title");
    else
      return NOT_EXISTS;
  }

  private boolean mustShow(String column) {
    return !columnsToHide.contains(column);
  }

  private String getNot0ValueOrDefault(int value) {
    return value != 0 ? Integer.toString(value) : NOT_EXISTS;
  }
}