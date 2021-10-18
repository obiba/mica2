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

import au.com.bytecode.opencsv.CSVWriter;
import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.Translator;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.spi.search.support.JoinQuery;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Component
@Scope("request")
public class SpecificStudyReportGenerator extends CsvReportGeneratorImpl {

  private static final Logger logger = LoggerFactory.getLogger(SpecificStudyReportGenerator.class);

  @Inject
  private JoinQueryExecutor joinQueryExecutor;

  @Inject
  private Searcher searcher;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private StudyService studyService;

  @Inject
  private NetworkService networkService;

  private Translator translator;
  private List<String> studyIds;
  private String locale;

  public void report(Translator translator, List<String> studyIds, String locale, OutputStream outputStream) {
    this.translator = translator;
    this.studyIds = studyIds.stream().sorted().collect(toList());
    this.locale = locale;
    this.write(outputStream);
  }

  public void report(String networkId, String locale, OutputStream outputStream) throws IOException {
    List<String> studyIds = networkService.findById(networkId).getStudyIds();
    Translator translator = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(locale, false));

    report(translator, studyIds, locale, outputStream);
  }

  public void report(String rqlQuery, OutputStream outputStream) throws IOException {

    JoinQuery joinQuery = searcher.makeJoinQuery(rqlQuery);

    List<String> studyIds = joinQueryExecutor.query(QueryType.STUDY, joinQuery)
        .getStudyResultDto()
        .getExtension(MicaSearch.StudyResultDto.result)
        .getSummariesList()
        .stream()
        .map(Mica.StudySummaryDto::getId)
        .collect(toList());
    Translator translator = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(joinQuery.getLocale(), false));

    report(translator, studyIds, joinQuery.getLocale(), outputStream);
  }

  @Override
  protected void writeHeader(CSVWriter writer) {

    List<String> line = new ArrayList<>();

    line.add(tr("report-group.study.name"));
    line.add(tr("report-group.study.acronym"));
    line.add(tr("report-group.study.type"));
    line.add(tr("report-group.study.status"));
    line.add(tr("report-group.study.country"));
    line.add(tr("report-group.study.start-year"));
    line.add(tr("report-group.study.years-of-follow-up"));
    line.add(tr("report-group.study.study-design"));
    line.add(tr("report-group.study.recruitment-target"));
    line.add(tr("report-group.study.number-of-participants"));
    line.add(tr("report-group.study.number-of-bio-samples"));
    line.add(tr("report-group.study.population.name"));
    line.add(tr("report-group.study.population.source-of-recruitment"));
    line.add(tr("report-group.study.population.minimum-age"));
    line.add(tr("report-group.study.population.maximum-age"));
    line.add(tr("report-group.study.population.number-of-participants"));
    line.add(tr("report-group.study.population.number-of-bio-samples"));
    line.add(tr("report-group.study.population.number-of-data-collection-events"));

    writer.writeNext(line.toArray(new String[line.size()]));
  }

  @Override
  protected void writeEachLine(CSVWriter writer) {

    for (String studyId : studyIds) {
      BaseStudy publishedStudy = publishedStudyService.findById(studyId);

      if (publishedStudy != null) {

        List<String> publishedStudyDetails = generatePublishedStudyDetails(publishedStudy);

        Iterator<Population> populationIterator = publishedStudy.getPopulations().iterator();

        if (!populationIterator.hasNext()) {
          writer.writeNext(publishedStudyDetails.toArray(new String[publishedStudyDetails.size()]));
        }

        while (populationIterator.hasNext()) {
          List<String> buildingCompleteLine = new ArrayList<>(publishedStudyDetails);
          Population next = populationIterator.next();
          buildingCompleteLine.addAll(generatePopulationDetails(next));
          writer.writeNext(buildingCompleteLine.toArray(new String[buildingCompleteLine.size()]));
        }
      } else {

        BaseStudy draftStudy = studyService.findStudy(studyId);
        if (draftStudy != null) {
          List<String> lineOfDratStudy = generateDraftStudyDetails(draftStudy);
          writer.writeNext(lineOfDratStudy.toArray(new String[lineOfDratStudy.size()]));
        }
      }
    }
  }

  private Map<String, Locale> getCountries() {
    return Arrays.stream(Locale.getAvailableLocales()).collect(Collectors.toMap(locale -> {
      try {
        return locale.getISO3Country();
      } catch (RuntimeException e) {
        return locale.getCountry();
      }
    }, Function.identity(), (a, b) -> a));
  }

  private String getCountryName(String iso3Country) {
    try {
      return getCountries().get(iso3Country).getDisplayCountry(new Locale(this.locale));
    } catch (RuntimeException e) {
      logger.debug("Translation of country %s is not available in java database", e);
      return iso3Country;
    }
  }

  private List<String> generateDraftStudyDetails(BaseStudy study) {
    List<String> line = new ArrayList<>();

    line.add(localizedField(study::getName));
    line.add(localizedField(study::getAcronym));
    line.add(field(() -> tr("global." + study.getResourcePath())));
    line.add(field(() -> tr("draft")));

    return line;
  }

  private List<String> generatePublishedStudyDetails(BaseStudy study) {
    List<String> line = new ArrayList<>();

    line.add(localizedField(study::getName));
    line.add(localizedField(study::getAcronym));
    line.add(field(() -> tr("global." + study.getResourcePath())));
    line.add(field(() -> tr("published")));
    line.add(arrayField(() -> study.getPopulations().stream().flatMap(population -> {
      Map<String, List<String>> selectionCriteria = (Map<String, List<String>>) population.getModel().get("selectionCriteria");
      return selectionCriteria.get("countriesIso").stream().map(this::getCountryName);
    }).distinct().sorted().collect(toList())));
    line.add(field(() -> study.getModel().get("startYear").toString()));
    line.add(field(() -> calculateYearsOfFollowUp(study)));
    line.add(field(() -> tr("study_taxonomy.vocabulary.methods-design.term." + ((Map<String, Object>) study.getModel().get("methods")).get("design").toString() + ".title")));
    line.add(arrayField(() -> ((List<String>) ((Map<String, Object>)
        study.getModel().get("methods")).get("recruitments"))
        .stream().map(m -> tr("study_taxonomy.vocabulary.methods-recruitments.term." + m + ".title")).sorted().collect(toList())));
    line.add(field(() -> ((Map<String, Object>) ((Map<String, Object>)
        study.getModel().get("numberOfParticipants")).get("participant")).get("number").toString()));
    line.add(field(() -> ((Map<String, Object>) ((Map<String, Object>)
        study.getModel().get("numberOfParticipants")).get("sample")).get("number").toString()));

    return line;
  }

  @NotNull
  private List<String> generatePopulationDetails(Population population) {
    List<String> line = new ArrayList<>();

    line.add(localizedField(() -> population.getName()));
    line.add(arrayField(() -> ((List<String>) ((Map<String, Object>) population.getModel().get("recruitment")).get("dataSources"))
        .stream().map(source -> tr("study_taxonomy.vocabulary.populations-recruitment-dataSources.term." + source + ".title")).sorted().collect(toList())));
    line.add(field(() -> ((Map<String, Object>) population.getModel().get("selectionCriteria")).get("ageMin").toString()));
    line.add(field(() -> ((Map<String, Object>) population.getModel().get("selectionCriteria")).get("ageMax").toString()));
    line.add(field(() -> ((Map<String, Object>) ((Map<String, Object>)
        population.getModel().get("numberOfParticipants")).get("participant")).get("number").toString()));
    line.add(field(() -> ((Map<String, Object>) ((Map<String, Object>)
        population.getModel().get("numberOfParticipants")).get("sample")).get("number").toString()));
    line.add(field(() -> Integer.toString(population.getDataCollectionEvents().size())));

    return line;
  }

  private String calculateYearsOfFollowUp(BaseStudy studySummaryDto) {
    if (studySummaryDto.getModel().get("startYear") == null)
      return "";
    int startYear = (int) studySummaryDto.getModel().get("startYear");
    int endYear = studySummaryDto.getModel().get("endYear") != null ? (int) studySummaryDto.getModel().get("endYear") : Calendar.getInstance().get(Calendar.YEAR);
    return Integer.toString(endYear - startYear);
  }

  private String tr(String key) {
    return translator.translate(key);
  }

  private String field(Callable<String> field) {
    try {
      return field.call();
    } catch (Exception e) {
      logger.debug("Error while generating csv custom report", e);
      return "";
    }
  }

  private String localizedField(Callable<LocalizedString> field) {
    return field(() -> field.call().get(this.locale));
  }

  private String arrayField(final Callable<List<String>> field) {
    return field(() -> {
      List<String> fields = field.call();
      return StringUtils.arrayToDelimitedString(fields.toArray(new String[fields.size()]), ", ");
    });
  }
}
