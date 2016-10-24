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

public class DatasetsCsvReportGenerator implements CsvReportGenerator {

  private static final String NOT_EXISTS = "-";

  private List<String> columnsToHide;
  private List<Mica.DatasetDto> datasetDtos;
  private Translator translator;

  public DatasetsCsvReportGenerator(MicaSearch.JoinQueryResultDto queryResult, List<String> columnsToHide, Translator translator) {
    this.columnsToHide = columnsToHide;
    this.datasetDtos = queryResult.getDatasetResultDto().getExtension(MicaSearch.DatasetResultDto.result).getDatasetsList();
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

    if (mustShow("showDatasetsAcronymColumn"))
      line.add("dataset.acronym");
    line.add("dataset.name");
    if (mustShow("showDatasetsTypeColumn"))
      line.add("dataset.type");

    if (mustShow("showDatasetsNetworkColumn"))
      line.add("dataset.networks");
    if (mustShow("showDatasetsStudiesColumn"))
      line.add("dataset.studies");
    if (mustShow("showDatasetsVariablesColumn"))
      line.add("dataset.variables");

    String[] translatedLine = line.stream().map(key -> translator.translate(key)).toArray(String[]::new);

    writer.writeNext(translatedLine);
  }

  private void writeEachLine(CSVWriter writer) {

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
    if (datasetDto.hasExtension(Mica.HarmonizationDatasetDto.type))
      return translator.translate("dataset.type.harmonization");
    else if (datasetDto.hasExtension(Mica.StudyDatasetDto.type))
      return translator.translate("dataset.type.study");
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
