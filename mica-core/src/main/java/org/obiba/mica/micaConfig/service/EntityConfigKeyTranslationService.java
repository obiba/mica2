package org.obiba.mica.micaConfig.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.PrefixedValueTranslator;
import org.obiba.core.translator.TranslationUtils;
import org.obiba.core.translator.Translator;
import org.obiba.mica.core.support.RegexHashMap;
import org.obiba.mica.micaConfig.domain.DataCollectionEventConfig;
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.obiba.mica.micaConfig.domain.HarmonizationDatasetConfig;
import org.obiba.mica.micaConfig.domain.HarmonizationPopulationConfig;
import org.obiba.mica.micaConfig.domain.HarmonizationStudyConfig;
import org.obiba.mica.micaConfig.domain.NetworkConfig;
import org.obiba.mica.micaConfig.domain.PopulationConfig;
import org.obiba.mica.micaConfig.domain.ProjectConfig;
import org.obiba.mica.micaConfig.domain.StudyConfig;
import org.obiba.mica.micaConfig.domain.StudyDatasetConfig;
import org.springframework.stereotype.Component;

import net.minidev.json.JSONArray;

@Component
public class EntityConfigKeyTranslationService {

  private final MicaConfigService micaConfigService;

  private final DataCollectionEventConfigService dataCollectionEventConfigService;
  private final HarmonizationDatasetConfigService harmonizationDatasetConfigService;
  private final HarmonizationPopulationConfigService harmonizationPopulationConfigService;
  private final HarmonizationStudyConfigService harmonizationStudyConfigService;
  private final IndividualStudyConfigService individualStudyConfigService;
  private final NetworkConfigService networkConfigService;
  private final PopulationConfigService populationConfigService;
  private final ProjectConfigService projectConfigService;
  private final StudyDatasetConfigService studyDatasetConfigService;

  @Inject
  public EntityConfigKeyTranslationService(MicaConfigService micaConfigService,
      DataCollectionEventConfigService dataCollectionEventConfigService,
      HarmonizationDatasetConfigService harmonizationDatasetConfigService,
      HarmonizationPopulationConfigService harmonizationPopulationConfigService,
      HarmonizationStudyConfigService harmonizationStudyConfigService,
      IndividualStudyConfigService individualStudyConfigService,
      NetworkConfigService networkConfigService,
      PopulationConfigService populationConfigService,
      ProjectConfigService projectConfigService,
      StudyDatasetConfigService studyDatasetConfigService) {
    this.micaConfigService = micaConfigService;
    this.dataCollectionEventConfigService = dataCollectionEventConfigService;
    this.harmonizationDatasetConfigService = harmonizationDatasetConfigService;
    this.harmonizationPopulationConfigService = harmonizationPopulationConfigService;
    this.harmonizationStudyConfigService = harmonizationStudyConfigService;
    this.individualStudyConfigService = individualStudyConfigService;
    this.networkConfigService = networkConfigService;
    this.populationConfigService = populationConfigService;
    this.projectConfigService = projectConfigService;
    this.studyDatasetConfigService = studyDatasetConfigService;
  }

  public RegexHashMap getCompleteConfigTranslationMap(String serviceTypename, String locale) {
    RegexHashMap translationMap = new RegexHashMap();

    switch (serviceTypename) {
      case "individual-study":

        Optional<StudyConfig> optionalIndividualStudySchemaForm = individualStudyConfigService.findComplete();
        Optional<PopulationConfig> optionalPopulationSchemaForm = populationConfigService.findComplete();
        Optional<DataCollectionEventConfig> optionalDataCollectionEventSchemaForm = dataCollectionEventConfigService.findComplete();

        if (optionalIndividualStudySchemaForm.isPresent()) {
          StudyConfig individualStudySchemaForm = optionalIndividualStudySchemaForm.get();
          translateSchemaForm(individualStudySchemaForm, locale);

          translationMap.putAll(getTranslationMap(individualStudySchemaForm, ""));
        }

        if (optionalPopulationSchemaForm.isPresent()) {
          PopulationConfig populationSchemaForm = optionalPopulationSchemaForm.get();
          translateSchemaForm(populationSchemaForm, locale);

          translationMap.putAll(getTranslationMap(populationSchemaForm, "^populations\\[\\d+\\]\\."));
        }

        if (optionalDataCollectionEventSchemaForm.isPresent()) {
          DataCollectionEventConfig dataCollectionEventSchemaForm = optionalDataCollectionEventSchemaForm.get();
          translateSchemaForm(dataCollectionEventSchemaForm, locale);

          translationMap.putAll(getTranslationMap(dataCollectionEventSchemaForm, "^populations\\[\\d+\\]\\.dataCollectionEvents\\[\\d+\\]\\."));
        }

        break;

      case "harmonization-study":

        Optional<HarmonizationStudyConfig> optionalHarmonizationStudySchemaForm = harmonizationStudyConfigService.findComplete();
        Optional<HarmonizationPopulationConfig> optionalHarmonizationPopulationSchemaForm = harmonizationPopulationConfigService.findComplete();

        if (optionalHarmonizationStudySchemaForm.isPresent()) {
          HarmonizationStudyConfig harmonizationStudySchemaForm = optionalHarmonizationStudySchemaForm.get();
          translateSchemaForm(harmonizationStudySchemaForm, locale);

          translationMap.putAll(getTranslationMap(harmonizationStudySchemaForm, ""));
        }

        if (optionalHarmonizationPopulationSchemaForm.isPresent()) {
          HarmonizationPopulationConfig harmonizationPopulationSchemaForm = optionalHarmonizationPopulationSchemaForm.get();
          translateSchemaForm(harmonizationPopulationSchemaForm, locale);

          translationMap.putAll(getTranslationMap(harmonizationPopulationSchemaForm, "^populations\\[\\d+\\]\\."));
        }

        break;

      case "network":
        Optional<NetworkConfig> optionalNetworkSchemaForm = networkConfigService.findComplete();

        if (optionalNetworkSchemaForm.isPresent()) {
          NetworkConfig networkSchemaForm = optionalNetworkSchemaForm.get();
          translateSchemaForm(networkSchemaForm, locale);

          translationMap.putAll(getTranslationMap(networkSchemaForm, ""));
        }

        break;

      case "collected-dataset":
        Optional<StudyDatasetConfig> optionalStudyDatasetSchemaForm = studyDatasetConfigService.findComplete();

        if (optionalStudyDatasetSchemaForm.isPresent()) {
          StudyDatasetConfig studyDatasetSchemaForm = optionalStudyDatasetSchemaForm.get();
          translateSchemaForm(studyDatasetSchemaForm, locale);

          translationMap.putAll(getTranslationMap(studyDatasetSchemaForm, ""));
        }

        break;

      case "harmonized-dataset":
        Optional<HarmonizationDatasetConfig> optionalHarmonizationDatasetSchemaForm = harmonizationDatasetConfigService.findComplete();

        if (optionalHarmonizationDatasetSchemaForm.isPresent()) {
          HarmonizationDatasetConfig harmonizationDatasetSchemaForm = optionalHarmonizationDatasetSchemaForm.get();
          translateSchemaForm(harmonizationDatasetSchemaForm, locale);

          translationMap.putAll(getTranslationMap(harmonizationDatasetSchemaForm, ""));
        }

        break;

      case "project":
        Optional<ProjectConfig> optionalProjectSchemaForm = projectConfigService.findComplete();

        if (optionalProjectSchemaForm.isPresent()) {
          ProjectConfig projectSchemaForm = optionalProjectSchemaForm.get();
          translateSchemaForm(projectSchemaForm, locale);

          translationMap.putAll(getTranslationMap(projectSchemaForm, ""));
        }

        break;

      default:
        break;
    }

    return translationMap;
  }

  private void translateSchemaForm(EntityConfig config, String locale) {
    Translator translator = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(locale, false));
    translator = new PrefixedValueTranslator(translator);

    TranslationUtils translationUtils = new TranslationUtils();
    config.setSchema(translationUtils.translate(config.getSchema(), translator));
    config.setDefinition(translationUtils.translate(config.getDefinition(), translator));
  }

  private RegexHashMap getTranslationMap(EntityConfig config, String prefix) {
    String string = config.getSchema();

    JSONArray array = JsonPath.using(Configuration.builder().options(Option.AS_PATH_LIST).build()).parse(string).read("$..*");

    RegexHashMap map = new RegexHashMap();

    List<String> locales = micaConfigService.getLocales();
    if (locales == null || locales.size() == 0) locales = Arrays.asList("en");
    String joinedLocales = locales.stream().map(locale -> "\\." + locale).collect(Collectors.joining("|"));

    array.stream().map(Object::toString)
    .filter(key -> key.endsWith("['title']"))
    .forEach(key -> {
      Object read = JsonPath.parse(string).read(key);
      if (read != null) {
        String cleanKey = key.replaceAll("(\\$\\[')|('\\])", "").replaceAll("(\\[')", ".").replaceAll("\\.title$", "").replaceAll("^properties\\.", "").replaceAll("\\.properties", "");

        String processedKey = (cleanKey.startsWith("_") ? prefix : prefix + "model\\.") + Pattern.quote((cleanKey.startsWith("_") ? cleanKey.substring(1) : cleanKey)) + "(\\[\\d+\\])?" + "(" + joinedLocales + ")?";

        map.put(processedKey, read.toString());
      }
    });

    return map;
  }

}