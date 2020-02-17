package org.obiba.mica.core.upgrade;

import org.obiba.mica.micaConfig.service.TaxonomyConfigService;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ElasticsearchMigrationUpgrade implements UpgradeStep {

  private final TaxonomyConfigService taxonomyConfigService;

  private final List<String> networkVocabularyKeyword = Stream.of("id", "studyIds").collect(Collectors.toList());
  private final List<String> networkVocabularyString = Stream.of("investigator", "contact").collect(Collectors.toList());

  private final List<String> studyVocabularyKeyword = Stream.of("id", "populations-id", "populations-dataCollectionEvents-id", "populations-dataCollectionEvents-end", "methods-design", "populations-selectionCriteria-countriesIso", "populations-selectionCriteria-gender", "populations-selectionCriteria-pregnantWomen", "populations-selectionCriteria-newborn", "populations-selectionCriteria-twins", "methods-recruitments", "populations-recruitment-dataSources", "populations-dataCollectionEvents-dataSources", "populations-recruitment-generalPopulationSources", "populations-recruitment-specificPopulationSources", "populations-dataCollectionEvents-bioSamples", "populations-dataCollectionEvents-administrativeDatabases", "access_data", "access_bio_samples", "access_other", "className").collect(Collectors.toList());
  private final List<String> studyVocabularyString = Stream.of("opal", "investigator", "contact").collect(Collectors.toList());

  private final List<String> datasetVocabularyKeyword = Stream.of("id", "entityType", "className").collect(Collectors.toList());
  private final List<String> datasetVocabularyString = new ArrayList<>();

  private final List<String> variableVocabularyKeyword = Stream.of("studyId", "populationId", "dceId", "datasetId", "sets", "mimeType", "variableType", "entityType", "valueType", "nature").collect(Collectors.toList());
  private final List<String> variableVocabularyString = Stream.of("name").collect(Collectors.toList());

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public ElasticsearchMigrationUpgrade(TaxonomyConfigService taxonomyConfigService) {
    this.taxonomyConfigService = taxonomyConfigService;
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 9, 0);
  }

  @Override
  public void execute(Version version) {
    updateTaxonomies();
  }

  private void updateTaxonomies() {
    updateTaxonomy(TaxonomyTarget.NETWORK, networkVocabularyKeyword, networkVocabularyString);
    updateTaxonomy(TaxonomyTarget.STUDY, studyVocabularyKeyword, studyVocabularyString);
    updateTaxonomy(TaxonomyTarget.DATASET, datasetVocabularyKeyword, datasetVocabularyString);
    updateTaxonomy(TaxonomyTarget.VARIABLE, variableVocabularyKeyword, variableVocabularyString);
  }

  private void updateTaxonomy(TaxonomyTarget target, List<String> vocabularyKeyword, List<String> vocabularyString) {
    Taxonomy taxonomy = taxonomyConfigService.findByTarget(target);

    if (taxonomy.hasVocabularies()) {
      logger.info("Updating '{}' taxonomy's vocabularies", target.name());

      taxonomy.getVocabularies().forEach(vocabulary -> {
        if (vocabularyKeyword.contains(vocabulary.getName())) {
          vocabulary.addAttribute("type", "keyword");
        } else if (vocabularyString.contains(vocabulary.getName())) {
          vocabulary.addAttribute("type", "string");
        }
      });

      taxonomyConfigService.update(target, taxonomy);
    }
  }
}
