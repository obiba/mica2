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
import org.obiba.mica.web.model.Mica.StudySummaryDto;
import org.obiba.mica.web.model.MicaSearch;

import au.com.bytecode.opencsv.CSVWriter;

public class DatasetDtosCsvReportGenerator extends CsvReportGenerator {

  private static final String NOT_EXISTS = "-";

  private List<String> columnsToHide;
  private List<Mica.DatasetDto> datasetDtos;
  private Translator translator;
  private final boolean forHarmonization;
  private final String locale;

  public DatasetDtosCsvReportGenerator(boolean forHarmonization, MicaSearch.JoinQueryResultDto queryResult, List<String> columnsToHide, String locale, Translator translator) {
    this.forHarmonization = forHarmonization;
    this.columnsToHide = columnsToHide;
    this.datasetDtos = queryResult.getDatasetResultDto().getExtension(MicaSearch.DatasetResultDto.result).getDatasetsList();
    this.translator = translator;
    this.locale = locale;
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
      line.add(this.forHarmonization ? "search.study.harmonization" : "search.study.label");
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

    Mica.CountStatsDto stats = datasetDto.getCountStats();

    if (mustShow("showDatasetsAcronymColumn"))
      line.add(getLocalizedStringFor(datasetDto.getAcronymList(), locale, datasetDto.getAcronym(0)).getValue());
    line.add(getLocalizedStringFor(datasetDto.getNameList(), locale, datasetDto.getName(0)).getValue());
    if (mustShow("showDatasetsTypeColumn"))
      line.add(findType(datasetDto));

    if (mustShow("showDatasetsNetworkColumn"))
      line.add(getNot0ValueOrDefault(stats.getNetworks()));
    if (mustShow("showDatasetsStudiesColumn"))
      line.add(findOpalTableStudyAcronym(datasetDto));
    if (mustShow("showDatasetsVariablesColumn"))
      line.add(getNot0ValueOrDefault(stats.getVariables()));

    return line;
  }

  private String findType(Mica.DatasetDto datasetDto) {
    if (datasetDto.hasProtocol())
      return translator.translate("dataset_taxonomy.vocabulary.className.term.HarmonizationDataset.title");
    else if (datasetDto.hasCollected())
      return translator.translate("dataset_taxonomy.vocabulary.className.term.StudyDataset.title");
    else
      return NOT_EXISTS;
  }

  private String findOpalTableStudyAcronym(Mica.DatasetDto datasetDto) {
    if (datasetDto.hasProtocol()) {
      StudySummaryDto harmoStudySummary = datasetDto.getProtocol().getHarmonizationTable().getStudySummary();
      return getLocalizedStringFor(harmoStudySummary.getAcronymList(), locale, harmoStudySummary.getAcronym(0)).getValue();
    } else if (datasetDto.hasCollected()) {
      StudySummaryDto studySummary = datasetDto.getCollected().getStudyTable().getStudySummary();
      return getLocalizedStringFor(studySummary.getAcronymList(), locale, studySummary.getAcronym(0)).getValue();
    } else
      return NOT_EXISTS;
  }

  private boolean mustShow(String column) {
    return !columnsToHide.contains(column);
  }

  private String getNot0ValueOrDefault(int value) {
    return value != 0 ? Integer.toString(value) : NOT_EXISTS;
  }
}
