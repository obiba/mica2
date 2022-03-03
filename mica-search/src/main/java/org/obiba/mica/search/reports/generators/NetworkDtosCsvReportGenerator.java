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

import org.obiba.core.translator.Translator;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;

import au.com.bytecode.opencsv.CSVWriter;

public class NetworkDtosCsvReportGenerator extends CsvReportGenerator {

  private static final String NOT_EXISTS = "-";

  private List<String> columnsToHide;
  private List<Mica.NetworkDto> networkDtos;
  private Translator translator;

  public NetworkDtosCsvReportGenerator(MicaSearch.JoinQueryResultDto queryResult, List<String> columnsToHide, Translator translator) {
    this.columnsToHide = columnsToHide;
    this.networkDtos = queryResult.getNetworkResultDto().getExtension(MicaSearch.NetworkResultDto.result).getNetworksList();
    this.translator = translator;
  }

  @Override
  protected void writeHeader(CSVWriter writer) {

    List<String> line = new ArrayList<>();

    line.add("acronym");
    line.add("name");

    if (mustShow("showNetworksStudiesColumn"))
      line.add("studies");

    String datasetsLabel = translator.translate("datasets");
    if (mustShow("showNetworksStudyDatasetsColumn"))
      line.add(String.format("%s:%s", datasetsLabel, translator.translate("search.dataset.collected")));
    if (mustShow("showNetworksHarmonizationDatasetsColumn"))
      line.add(String.format("%s:%s", datasetsLabel, translator.translate("search.dataset.harmonized")));

    if (mustShow("showNetworksVariablesColumn")) {
      String variablesLabel = translator.translate("variables");
      if (mustShow("showNetworksStudyVariablesColumn"))
        line.add(String.format("%s:%s", variablesLabel, translator.translate("search.variable.collected")));
      if (mustShow("showNetworksDataschemaVariablesColumn")) {
        line.add(String.format("%s:%s", variablesLabel, translator.translate("search.variable.dataschema")));
      }
    }

    String[] translatedLine = line.stream().map(key -> translator.translate(key)).toArray(String[]::new);

    writer.writeNext(translatedLine);
  }

  @Override
  protected void writeEachLine(CSVWriter writer) {

    for (Mica.NetworkDto networkDto : networkDtos) {
      List<String> lineContent = generateLineContent(networkDto);
      writer.writeNext(lineContent.toArray(new String[lineContent.size()]));
    }
  }

  private List<String> generateLineContent(Mica.NetworkDto networkDto) {

    List<String> line = new ArrayList<>();

    MicaSearch.CountStatsDto networkCountStats = networkDto.getExtension(MicaSearch.CountStatsDto.networkCountStats);

    line.add(networkDto.getAcronym(0).getValue());
    line.add(networkDto.getName(0).getValue());

    if (mustShow("showNetworksStudiesColumn"))
      line.add(getNot0ValueOrDefault(networkCountStats.getStudies()));

    if (mustShow("showNetworksStudyDatasetsColumn"))
      line.add(getNot0ValueOrDefault(networkCountStats.getStudyDatasets()));
    if (mustShow("showNetworksHarmonizationDatasetsColumn"))
      line.add(getNot0ValueOrDefault(networkCountStats.getHarmonizationDatasets()));

    if (mustShow("showNetworksVariablesColumn")) {
      if (mustShow("showNetworksStudyVariablesColumn"))
        line.add(getNot0ValueOrDefault(networkCountStats.getStudyVariables()));
      if (mustShow("showNetworksDataschemaVariablesColumn"))
        line.add(getNot0ValueOrDefault(networkCountStats.getDataschemaVariables()));
    }

    return line;
  }

  private boolean mustShow(String column) {
    return !columnsToHide.contains(column);
  }

  private String getNot0ValueOrDefault(int value) {
    return value != 0 ? Integer.toString(value) : NOT_EXISTS;
  }
}
