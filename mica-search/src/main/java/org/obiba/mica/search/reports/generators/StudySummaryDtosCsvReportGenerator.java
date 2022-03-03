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

import com.google.common.base.Strings;

import au.com.bytecode.opencsv.CSVWriter;

public class StudySummaryDtosCsvReportGenerator extends CsvReportGenerator {

  private static final String EXISTS = "X";
  private static final String NOT_EXISTS = "-";

  private List<String> columnsToHide;
  private List<Mica.StudySummaryDto> studySummaryDtos;
  private Translator translator;

  public StudySummaryDtosCsvReportGenerator(MicaSearch.JoinQueryResultDto queryResult, List<String> columnsToHide, Translator translator) {
    this.columnsToHide = columnsToHide;
    this.studySummaryDtos = queryResult.getStudyResultDto().getExtension(MicaSearch.StudyResultDto.result).getSummariesList();
    this.translator = translator;
  }

  @Override
  protected void writeHeader(CSVWriter writer) {

    List<String> line = new ArrayList<>();

    line.add("acronym");
    line.add("name");
    line.add("type");
    if (mustShow("showStudiesDesignColumn"))
      line.add("search.study.design");

    if (mustShow("showStudiesQuestionnaireColumn"))
      line.add("study_taxonomy.vocabulary.populations-dataCollectionEvents-dataSources.term.questionnaires.title");
    if (mustShow("showStudiesPmColumn"))
      line.add("study_taxonomy.vocabulary.populations-dataCollectionEvents-dataSources.term.physical_measures.title");
    if (mustShow("showStudiesBioColumn"))
      line.add("study_taxonomy.vocabulary.populations-dataCollectionEvents-dataSources.term.biological_samples.title");
    if (mustShow("showStudiesOtherColumn"))
      line.add("study_taxonomy.vocabulary.populations-dataCollectionEvents-dataSources.term.others.title");

    if (mustShow("showStudiesParticipantsColumn"))
      line.add("search.study.participants");
    if (mustShow("showStudiesNetworksColumn"))
      line.add("networks");

    String individualLabel = translator.translate("search.study.individual");
    if (mustShow("showStudiesStudyDatasetsColumn"))
      line.add(String.format("%s:%s", individualLabel, translator.translate("datasets")));
    if (mustShow("showStudiesStudyVariablesColumn"))
      line.add(String.format("%s:%s", individualLabel, translator.translate("variables")));

    if (mustShow("showStudiesVariablesColumn")) {
      String harmonizationLabel = translator.translate("search.study.harmonization");
      if (mustShow("showStudiesHarmonizationDatasetsColumn"))
        line.add(String.format("%s:%s", harmonizationLabel, translator.translate("global.protocols")));
      if (mustShow("showStudiesDataschemaVariablesColumn")) {
        line.add(String.format("%s:%s", harmonizationLabel, translator.translate("variables")));
      }
    }

    String[] translatedLine = line.stream().map(key -> translator.translate(key)).toArray(String[]::new);

    writer.writeNext(translatedLine);
  }

  @Override
  protected void writeEachLine(CSVWriter writer) {

    for (Mica.StudySummaryDto studySummaryDto : studySummaryDtos) {
      List<String> lineContent = generateLineContent(studySummaryDto);
      writer.writeNext(lineContent.toArray(new String[lineContent.size()]));
    }
  }

  private List<String> generateLineContent(Mica.StudySummaryDto studySummaryDto) {
    return "harmonization-study".equals(studySummaryDto.getStudyResourcePath())
      ? generateHarmonizationStudyLineContent(studySummaryDto)
      : generateIndividualStudyLineContent(studySummaryDto);
  }

  private List<String> generateHarmonizationStudyLineContent(Mica.StudySummaryDto studySummaryDto) {
    List<String> line = new ArrayList<>();

    line.add(studySummaryDto.getAcronym(0).getValue());
    line.add(studySummaryDto.getName(0).getValue());
    line.add(translator.translate("search.study.harmonization"));

    if (mustShow("showStudiesDesignColumn")) {
      line.add(NOT_EXISTS);
    }

    if (mustShow("showStudiesQuestionnaireColumn"))
      line.add(NOT_EXISTS);
    if (mustShow("showStudiesPmColumn"))
      line.add(NOT_EXISTS);
    if (mustShow("showStudiesBioColumn"))
      line.add(NOT_EXISTS);
    if (mustShow("showStudiesOtherColumn"))
      line.add(NOT_EXISTS);

    if (mustShow("showStudiesParticipantsColumn"))
      line.add(NOT_EXISTS);
    if (mustShow("showStudiesNetworksColumn"))
      line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getNetworks()));

    if (mustShow("showStudiesStudyDatasetsColumn"))
      line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getStudyDatasets()));
    if (mustShow("showStudiesVariablesColumn") && mustShow("showStudiesStudyVariablesColumn"))
      line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getStudyVariables()));

    if (mustShow("showStudiesHarmonizationDatasetsColumn"))
      line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getHarmonizationDatasets()));
    if (mustShow("showStudiesVariablesColumn") && mustShow("showStudiesDataschemaVariablesColumn"))
      line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getDataschemaVariables()));

    return line;
  }

  private List<String> generateIndividualStudyLineContent(Mica.StudySummaryDto studySummaryDto) {

    List<String> line = new ArrayList<>();

    line.add(studySummaryDto.getAcronym(0).getValue());
    line.add(studySummaryDto.getName(0).getValue());
    line.add(translator.translate("search.study.individual"));

    if (mustShow("showStudiesDesignColumn")) {
      if (Strings.isNullOrEmpty(studySummaryDto.getDesign())) {
        line.add(NOT_EXISTS);
      } else {
        line.add(translator.translate(
          String.format("study_taxonomy.vocabulary.methods-design.term.%s.title", studySummaryDto.getDesign())));
      }
    }

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
    if (mustShow("showStudiesVariablesColumn") && mustShow("showStudiesStudyVariablesColumn"))
      line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getStudyVariables()));

    if (mustShow("showStudiesHarmonizationDatasetsColumn"))
      line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getHarmonizationDatasets()));
    if (mustShow("showStudiesVariablesColumn") && mustShow("showStudiesDataschemaVariablesColumn"))
      line.add(getNot0ValueOrDefault(studySummaryDto.getExtension(MicaSearch.CountStatsDto.studyCountStats).getDataschemaVariables()));

    return line;
  }

  private boolean mustShow(String column) {
    return !columnsToHide.contains(column);
  }

  private String getNot0ValueOrDefault(int value) {
    return value != 0 ? Integer.toString(value) : NOT_EXISTS;
  }
}
