package org.obiba.mica.micaConfig.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.base.Strings;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.PrefixedValueTranslator;
import org.obiba.core.translator.TranslationUtils;
import org.obiba.core.translator.Translator;
import org.obiba.mica.core.support.RegexHashMap;
import org.obiba.mica.micaConfig.domain.*;
import org.springframework.stereotype.Component;

import net.minidev.json.JSONArray;

@Component
public class EntityConfigKeyTranslationService {

  private final MicaConfigService micaConfigService;

  private final DataCollectionEventConfigService dataCollectionEventConfigService;
  private final HarmonizationDatasetConfigService harmonizationDatasetConfigService;
  private final HarmonizationStudyConfigService harmonizationStudyConfigService;
  private final IndividualStudyConfigService individualStudyConfigService;
  private final NetworkConfigService networkConfigService;
  private final PopulationConfigService populationConfigService;
  private final ProjectConfigService projectConfigService;
  private final StudyDatasetConfigService studyDatasetConfigService;

  private final DataAccessFormService dataAccessFormService;

  private final DataAccessFeasibilityFormService dataAccessFeasibilityFormService;

  private final DataAccessAmendmentFormService dataAccessAmendmentFormService;

  private final Map<String, String> beanFieldToTranslationKeyMap;

  @Inject
  public EntityConfigKeyTranslationService(MicaConfigService micaConfigService,
                                           DataCollectionEventConfigService dataCollectionEventConfigService,
                                           HarmonizationDatasetConfigService harmonizationDatasetConfigService,
                                           HarmonizationStudyConfigService harmonizationStudyConfigService,
                                           IndividualStudyConfigService individualStudyConfigService,
                                           NetworkConfigService networkConfigService,
                                           PopulationConfigService populationConfigService,
                                           ProjectConfigService projectConfigService,
                                           StudyDatasetConfigService studyDatasetConfigService,
                                           DataAccessFormService dataAccessFormService,
                                           DataAccessFeasibilityFormService dataAccessFeasibilityFormService,
                                           DataAccessAmendmentFormService dataAccessAmendmentFormService) {
    this.micaConfigService = micaConfigService;
    this.dataCollectionEventConfigService = dataCollectionEventConfigService;
    this.harmonizationDatasetConfigService = harmonizationDatasetConfigService;
    this.harmonizationStudyConfigService = harmonizationStudyConfigService;
    this.individualStudyConfigService = individualStudyConfigService;
    this.networkConfigService = networkConfigService;
    this.populationConfigService = populationConfigService;
    this.projectConfigService = projectConfigService;
    this.studyDatasetConfigService = studyDatasetConfigService;
    this.dataAccessFormService = dataAccessFormService;
    this.dataAccessFeasibilityFormService = dataAccessFeasibilityFormService;
    this.dataAccessAmendmentFormService = dataAccessAmendmentFormService;

    String joinedLocales = getJoinedLocales();

    beanFieldToTranslationKeyMap = new HashMap<>();
    // collected dataset
    beanFieldToTranslationKeyMap.put("studyTable\\.name" + "(" + joinedLocales + ")?", "dataset.table-name");
    beanFieldToTranslationKeyMap.put("studyTable\\.description" + "(" + joinedLocales + ")?", "dataset.table-description");
    beanFieldToTranslationKeyMap.put("studyTable\\.project", "dataset.project");
    beanFieldToTranslationKeyMap.put("studyTable\\.table", "dataset.table");
    beanFieldToTranslationKeyMap.put("studyTable\\.studyId", "study.label");
    beanFieldToTranslationKeyMap.put("studyTable\\.populationId", "study.population");
    beanFieldToTranslationKeyMap.put("studyTable\\.dataCollectionEventId", "study.data-collection-event");

    // harmonized dataset
    beanFieldToTranslationKeyMap.put("studyTables\\[\\d+\\]\\.name" + "(" + joinedLocales + ")?", "dataset.table-name");
    beanFieldToTranslationKeyMap.put("studyTables\\[\\d+\\]\\.description" + "(" + joinedLocales + ")?", "dataset.table-description");
    beanFieldToTranslationKeyMap.put("studyTables\\[\\d+\\]\\.project", "dataset.project");
    beanFieldToTranslationKeyMap.put("studyTables\\[\\d+\\]\\.table", "dataset.table");
    beanFieldToTranslationKeyMap.put("studyTables\\[\\d+\\]\\.studyId", "study.label");
    beanFieldToTranslationKeyMap.put("studyTables\\[\\d+\\]\\.populationId", "study.population");
    beanFieldToTranslationKeyMap.put("studyTables\\[\\d+\\]\\.dataCollectionEventId", "study.data-collection-event");

    beanFieldToTranslationKeyMap.put("harmonizationTables\\[\\d+\\]\\.name" + "(" + joinedLocales + ")?", "dataset.table-name");
    beanFieldToTranslationKeyMap.put("harmonizationTables\\[\\d+\\]\\.description" + "(" + joinedLocales + ")?", "dataset.table-description");
    beanFieldToTranslationKeyMap.put("harmonizationTables\\[\\d+\\]\\.project", "dataset.project");
    beanFieldToTranslationKeyMap.put("harmonizationTables\\[\\d+\\]\\.table", "dataset.table");
    beanFieldToTranslationKeyMap.put("harmonizationTables\\[\\d+\\]\\.studyId", "study.label");
    beanFieldToTranslationKeyMap.put("harmonizationTables\\[\\d+\\]\\.populationId", "study.population");

    beanFieldToTranslationKeyMap.put("harmonizationTable\\.project", "dataset.project");
    beanFieldToTranslationKeyMap.put("harmonizationTable\\.table", "dataset.table");
    beanFieldToTranslationKeyMap.put("harmonizationTable\\.studyId", "study.label");
    beanFieldToTranslationKeyMap.put("harmonizationTable\\.populationId", "study.population");

    // network
    beanFieldToTranslationKeyMap.put("studyIds\\[\\d+\\]", "studies");

    // study
    beanFieldToTranslationKeyMap.put("populations\\[\\d+\\].dataCollectionEvents\\[\\d+\\]\\.start\\.yearMonth", "study.start");
    beanFieldToTranslationKeyMap.put("populations\\[\\d+\\].dataCollectionEvents\\[\\d+\\]\\.end\\.yearMonth", "study.end");
  }

  public RegexHashMap getCompleteConfigTranslationMap(String serviceTypename, String locale) {
    RegexHashMap translationMap = new RegexHashMap();

    Translator translator = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(locale, false));

    beanFieldToTranslationKeyMap.forEach((key, translationKey) -> translationMap.put(key, translator.translate(translationKey)));

    if (!Strings.isNullOrEmpty(serviceTypename)) {
      switch (serviceTypename) {
        case "individual-study":

          Optional<StudyConfig> optionalIndividualStudySchemaForm = individualStudyConfigService.findComplete();
          Optional<PopulationConfig> optionalPopulationSchemaForm = populationConfigService.findComplete();
          Optional<DataCollectionEventConfig> optionalDataCollectionEventSchemaForm = dataCollectionEventConfigService.findComplete();

          if (optionalIndividualStudySchemaForm.isPresent()) {
            StudyConfig individualStudySchemaForm = optionalIndividualStudySchemaForm.get();
            translateSchemaForm(translator, individualStudySchemaForm);

            translationMap.putAll(getTranslationMap(individualStudySchemaForm, ""));
          }

          if (optionalPopulationSchemaForm.isPresent()) {
            PopulationConfig populationSchemaForm = optionalPopulationSchemaForm.get();
            translateSchemaForm(translator, populationSchemaForm);

            translationMap.putAll(getTranslationMap(populationSchemaForm, "^populations\\[\\d+\\]\\."));
          }

          if (optionalDataCollectionEventSchemaForm.isPresent()) {
            DataCollectionEventConfig dataCollectionEventSchemaForm = optionalDataCollectionEventSchemaForm.get();
            translateSchemaForm(translator, dataCollectionEventSchemaForm);

            translationMap.putAll(getTranslationMap(dataCollectionEventSchemaForm, "^populations\\[\\d+\\]\\.dataCollectionEvents\\[\\d+\\]\\."));
          }

          break;

        case "harmonization-study":

          Optional<HarmonizationStudyConfig> optionalHarmonizationStudySchemaForm = harmonizationStudyConfigService.findComplete();

          if (optionalHarmonizationStudySchemaForm.isPresent()) {
            HarmonizationStudyConfig harmonizationStudySchemaForm = optionalHarmonizationStudySchemaForm.get();
            translateSchemaForm(translator, harmonizationStudySchemaForm);

            translationMap.putAll(getTranslationMap(harmonizationStudySchemaForm, ""));
          }

          break;

        case "network":
          Optional<NetworkConfig> optionalNetworkSchemaForm = networkConfigService.findComplete();

          if (optionalNetworkSchemaForm.isPresent()) {
            NetworkConfig networkSchemaForm = optionalNetworkSchemaForm.get();
            translateSchemaForm(translator, networkSchemaForm);

            translationMap.putAll(getTranslationMap(networkSchemaForm, ""));
          }

          break;

        case "collected-dataset":
          Optional<StudyDatasetConfig> optionalStudyDatasetSchemaForm = studyDatasetConfigService.findComplete();

          if (optionalStudyDatasetSchemaForm.isPresent()) {
            StudyDatasetConfig studyDatasetSchemaForm = optionalStudyDatasetSchemaForm.get();
            translateSchemaForm(translator, studyDatasetSchemaForm);

            translationMap.putAll(getTranslationMap(studyDatasetSchemaForm, ""));
          }

          break;

        case "harmonized-dataset":
          Optional<HarmonizationDatasetConfig> optionalHarmonizationDatasetSchemaForm = harmonizationDatasetConfigService.findComplete();

          if (optionalHarmonizationDatasetSchemaForm.isPresent()) {
            HarmonizationDatasetConfig harmonizationDatasetSchemaForm = optionalHarmonizationDatasetSchemaForm.get();
            translateSchemaForm(translator, harmonizationDatasetSchemaForm);

            translationMap.putAll(getTranslationMap(harmonizationDatasetSchemaForm, ""));
          }

          break;

        case "project":
          Optional<ProjectConfig> optionalProjectSchemaForm = projectConfigService.findComplete();

          if (optionalProjectSchemaForm.isPresent()) {
            ProjectConfig projectSchemaForm = optionalProjectSchemaForm.get();
            translateSchemaForm(translator, projectSchemaForm);

            translationMap.putAll(getTranslationMap(projectSchemaForm, ""));
          }

          break;

        case "data-access-form":
          DataAccessForm dataAccessForm = dataAccessFormService.findLatest();
          translateSchemaForm(translator, dataAccessForm);
          translationMap.putAll(getTranslationMap(dataAccessForm, ""));
          break;

        case "data-access-feasibility":
          DataAccessFeasibilityForm dataAccessFeasibilityForm = dataAccessFeasibilityFormService.findLatest();
          translateSchemaForm(translator, dataAccessFeasibilityForm);
          translationMap.putAll(getTranslationMap(dataAccessFeasibilityForm, ""));
          break;

        case "data-access-amendment":
          DataAccessAmendmentForm dataAccessAmendmentForm = dataAccessAmendmentFormService.findLatest();
          translateSchemaForm(translator, dataAccessAmendmentForm);
          translationMap.putAll(getTranslationMap(dataAccessAmendmentForm, ""));
          break;

        default:
          break;
      }
    }

    return translationMap;
  }

  private void translateSchemaForm(Translator translator, EntityConfig config) {
    TranslationUtils translationUtils = new TranslationUtils();
    PrefixedValueTranslator prefixedValueTranslator = new PrefixedValueTranslator(translator);
    config.setSchema(translationUtils.translate(config.getSchema(), prefixedValueTranslator));
    config.setDefinition(translationUtils.translate(config.getDefinition(), prefixedValueTranslator));
  }

  private String getJoinedLocales() {
    List<String> locales = micaConfigService.getLocales();
    if (locales == null || locales.size() == 0) locales = Arrays.asList("en");
    return locales.stream().map(locale -> "\\." + locale).collect(Collectors.joining("|"));
  }

  private RegexHashMap getTranslationMap(EntityConfig config, String prefix) {
    String string = config.getSchema();

    JSONArray normalArray = JsonPath.using(Configuration.builder().options(Option.AS_PATH_LIST, Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS).build()).parse(string).read("$..*");
    JSONArray itemsArray = JsonPath.using(Configuration.builder().options(Option.AS_PATH_LIST, Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS).build()).parse(string).read("$..items");

    RegexHashMap map = new RegexHashMap();

    String joinedLocales = getJoinedLocales();

    normalArray.stream().map(Object::toString)
    .filter(key -> key.endsWith("['title']"))
    .forEach(key -> {
      Object read = JsonPath.parse(string).read(key);
      if (read != null) {
        String cleanKey = key.replaceAll("(\\$\\[')|('\\])", "").replaceAll("(\\[')", ".").replaceAll("\\.title$", "").replaceAll("^properties\\.", "").replaceAll("\\.properties", "");

        String processedKey = (cleanKey.startsWith("_") ? prefix : prefix + "(model\\.)?") + Pattern.quote((cleanKey.startsWith("_") ? cleanKey.substring(1) : cleanKey)) + "(\\[\\d+\\])?" + "(" + joinedLocales + ")?";

        map.put(processedKey, read.toString());
      }
    });

    itemsArray.stream().map(Object::toString)
    .forEach(itemkey -> {
      Object read = JsonPath.parse(string).read(itemkey);
      if (read != null && read instanceof List) {
        JSONArray array = (JSONArray) read;
        array.forEach(arrayItem -> {

          if (arrayItem != null && arrayItem instanceof Map) {
            Map itemMap = (Map) arrayItem;
            Object key = itemMap.get("key");
            Object name = itemMap.get("name");

            String cleanKey = itemkey.replaceAll("(\\$\\[')|('\\])", "").replaceAll("(\\[')", ".").replaceAll("\\.items$", "").replaceAll("^properties\\.", "").replaceAll("\\.properties", "") + "." + key.toString();

            String processedKey = (cleanKey.startsWith("_") ? prefix : prefix + "model\\.") + Pattern.quote((cleanKey.startsWith("_") ? cleanKey.substring(1) : cleanKey));

            map.put(processedKey, name);
          }
        });
      }
    });

    return map;
  }

}
