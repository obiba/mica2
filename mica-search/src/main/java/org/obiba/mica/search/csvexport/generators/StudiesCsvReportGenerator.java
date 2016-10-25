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

public class StudiesCsvReportGenerator implements CsvReportGenerator {

  private static final String EXISTS = "X";
  private static final String NOT_EXISTS = "-";

  private List<String> columnsToHide;
  private List<Mica.StudySummaryDto> studySummaryDtos;
  private Translator translator;

  public StudiesCsvReportGenerator(MicaSearch.JoinQueryResultDto queryResult, List<String> columnsToHide, Translator translator) {
    this.columnsToHide = columnsToHide;
    this.studySummaryDtos = queryResult.getStudyResultDto().getExtension(MicaSearch.StudyResultDto.result).getSummariesList();
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

    line.add("study.acronym");
    line.add("study.name");
    if (mustShow("showStudiesDesignColumn"))
      line.add("study.design");

    if (mustShow("showStudiesQuestionnaireColumn"))
      line.add("study.dataSourcesAvailable.questionnaires");
    if (mustShow("showStudiesPmColumn"))
      line.add("study.dataSourcesAvailable.physicalMesures");
    if (mustShow("showStudiesBioColumn"))
      line.add("study.dataSourcesAvailable.biosamples");
    if (mustShow("showStudiesOtherColumn"))
      line.add("study.dataSourcesAvailable.others");

    if (mustShow("showStudiesParticipantsColumn"))
      line.add("study.participants");
    if (mustShow("showStudiesNetworksColumn"))
      line.add("study.networks");

    if (mustShow("showStudiesStudyDatasetsColumn"))
      line.add("study.datasets.study");
    if (mustShow("showStudiesHarmonizationDatasetsColumn"))
      line.add("study.datasets.harmonization");

    if (mustShow("showStudiesVariablesColumn")) {
      if (mustShow("showStudiesStudyVariablesColumn"))
        line.add("study.variables.study");
      if (mustShow("showStudiesDataschemaVariablesColumn"))
        line.add("study.variables.dataSchema");
    }

    String[] translatedLine = line.stream().map(key -> translator.translate(key)).toArray(String[]::new);

    writer.writeNext(translatedLine);
  }

  private void writeEachLine(CSVWriter writer) {

    for (Mica.StudySummaryDto studySummaryDto : studySummaryDtos) {
      List<String> lineContent = generateLineContent(studySummaryDto);
      writer.writeNext(lineContent.toArray(new String[lineContent.size()]));
    }
  }

  private List<String> generateLineContent(Mica.StudySummaryDto studySummaryDto) {

    List<String> line = new ArrayList<>();

    line.add(studySummaryDto.getAcronym(0).getValue());
    line.add(studySummaryDto.getName(0).getValue());
    if (mustShow("showStudiesDesignColumn"))
      line.add(studySummaryDto.getDesigns(0));

    if (mustShow("showStudiesQuestionnaireColumn"))
      line.add(studySummaryDto.getDataSourcesList().contains("questionnaires") ? EXISTS : NOT_EXISTS);
    if (mustShow("showStudiesPmColumn"))
      line.add(studySummaryDto.getDataSourcesList().contains("physical_measures") ? EXISTS : NOT_EXISTS);
    if (mustShow("showStudiesBioColumn"))
      line.add(studySummaryDto.getDataSourcesList().contains("biological_samples") ? EXISTS : NOT_EXISTS);
    if (mustShow("showStudiesOtherColumn"))
      line.add(studySummaryDto.getDataSourcesList().contains("others") ? EXISTS : NOT_EXISTS);

    if (mustShow("showStudiesParticipantsColumn"))
      line.add(getNot0ValueOrDefault(studySummaryDto.getTargetNumber().getNumber()));
    if (mustShow("showStudiesNetworksColumn"))
      line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getNetworks()));

    if (mustShow("showStudiesStudyDatasetsColumn"))
      line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getStudyDatasets()));
    if (mustShow("showStudiesHarmonizationDatasetsColumn"))
      line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getHarmonizationDatasets()));

    if (mustShow("showStudiesVariablesColumn")) {
      if (mustShow("showStudiesStudyVariablesColumn"))
        line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getStudyVariables()));
      if (mustShow("showStudiesDataschemaVariablesColumn"))
        line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getDataschemaVariables()));
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
