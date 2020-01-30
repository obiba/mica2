package org.obiba.mica.core.upgrade;

import com.google.common.eventbus.EventBus;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.service.TaxonomyConfigService;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.mica.study.service.StudyService;
import org.obiba.opal.core.cfg.NoSuchVocabularyException;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.stream.Stream;

@Component
public class Mica380Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica380Upgrade.class);

  private final EventBus eventBus;

  private final TaxonomyConfigService taxonomyConfigService;

  private final StudyService studyService;

  private final NetworkService networkService;

  @Inject
  public Mica380Upgrade(EventBus eventBus,
                        TaxonomyConfigService taxonomyConfigService,
                        StudyService studyService, NetworkService networkService) {
    this.eventBus = eventBus;
    this.taxonomyConfigService = taxonomyConfigService;
    this.studyService = studyService;
    this.networkService = networkService;
  }


  @Override
  public String getDescription() {
    return "Upgrade data to 3.8.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 8, 0);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 3.8.0");

    try {
      logger.info("Ensure Study taxonomy's investigator and contact vocabulary are of type `text`");
      ensureVocabularyType(TaxonomyTarget.STUDY, "investigator", "contact");
      eventBus.post(new IndexStudiesEvent());
    } catch (RuntimeException e) {
      logger.error("Error occurred when ensuring Study taxonomy's investigator and contact vocabulary are of type `text`.", e);
    }

    try {
      logger.info("Ensure Network taxonomy's investigator and contact vocabulary are of type `text`");
      ensureVocabularyType(TaxonomyTarget.NETWORK, "investigator", "contact");
      eventBus.post(new IndexNetworksEvent());
    } catch (RuntimeException e) {
      logger.error("Error occurred when ensuring Network taxonomy's investigator and contact vocabulary are of type `text`.", e);
    }
  }

  private void ensureVocabularyType(TaxonomyTarget target, String... vocabularyNames) {
    Taxonomy taxonomy = taxonomyConfigService.findByTarget(target);
    try {
      boolean updated = false;
      for (String vocabularyName : vocabularyNames) {
        Vocabulary vocabulary = taxonomy.getVocabulary(vocabularyName);
        if (vocabulary != null && !"string".equals(vocabulary.getAttributeValue("type"))) {
          vocabulary.addAttribute("type", "string");
          updated = true;
        }
      }

      if (updated) {
        taxonomyConfigService.update(target, taxonomy);
      }
    } catch (NoSuchVocabularyException ignore) {
    }
  }

}
