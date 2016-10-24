/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.csvexport.generators;

import au.com.bytecode.opencsv.CSVWriter;
import org.obiba.core.translator.Translator;
import org.obiba.mica.search.csvexport.CsvReportGenerator;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class NetworkCsvReportGenerator implements CsvReportGenerator {

  private static final String NOT_EXISTS = "-";

  private List<String> columnsToHide;
  private List<Mica.NetworkDto> networkDtos;
  private Translator translator;

  public NetworkCsvReportGenerator(MicaSearch.JoinQueryResultDto queryResult, List<String> columnsToHide, Translator translator) {
    this.columnsToHide = columnsToHide;
    this.networkDtos = queryResult.getNetworkResultDto().getExtension(MicaSearch.NetworkResultDto.result).getNetworksList();
    this.translator = translator;
  }

  public void write(OutputStream outputStream) {

    try (CSVWriter writer = new CSVWriter(new PrintWriter(outputStream))) {

      writeHeader(writer);
      writeEachLine(writer);

    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void writeHeader(CSVWriter writer) {

    List<String> line = new ArrayList<>();

    line.add("network.acronym");
    line.add("network.name");

    if (mustShow("showNetworksStudiesColumn"))
      line.add("network.study");

    if (mustShow("showNetworksStudyDatasetColumn"))
      line.add("network.datasets.study");
    if (mustShow("showNetworksHarmonizationDatasetColumn"))
      line.add("network.datasets.harmonization");

    if (mustShow("showNetworksVariablesColumn")) {
      if (mustShow("showNetworksStudyVariablesColumn"))
        line.add("network.variables.study");
      if (mustShow("showNetworksDataschemaVariablesColumn"))
        line.add("network.variables.dataSchema");
    }

    String[] translatedLine = line.stream().map(key -> translator.translate(key)).toArray(String[]::new);

    writer.writeNext(translatedLine);
  }

  private void writeEachLine(CSVWriter writer) {

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

    if (mustShow("showNetworksStudyDatasetColumn"))
      line.add(getNot0ValueOrDefault(networkCountStats.getStudyDatasets()));
    if (mustShow("showNetworksHarmonizationDatasetColumn"))
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
