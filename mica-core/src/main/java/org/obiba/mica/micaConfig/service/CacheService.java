package org.obiba.mica.micaConfig.service;

import javax.inject.Inject;

import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.security.event.SubjectAclUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

@Component
public class CacheService {

  private static final Logger log = LoggerFactory.getLogger(CacheService.class);

  @Inject
  private Helper helper;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private EventBus eventBus;

  @CacheEvict(value="agate-subjects", allEntries = true)
  public void clearAgateSubjectsCache() {
    log.info("Clearing agate subjects cache");
  }

  @CacheEvict(value="opal-taxonomies", allEntries = true)
  public void clearOpalTaxonomiesCache() {
    log.info("Clearing opal taxonomies cache");
  }

  @CacheEvict(value="micaConfig", allEntries = true)
  public void clearMicaConfigCache() {
    log.info("Clearing mica config cache");
  }

  @CacheEvict(value="aggregations-metadata", allEntries = true)
  public void clearAggregationsMetadataCache() {
    log.info("Clearing aggregations metadata cache");
  }

  public void clearDatasetVariablesCache() {
    harmonizationDatasetService.findAllDatasets().forEach(dataset -> {
      helper.clearDatasetVariablesCache(dataset);
    });

    studyDatasetService.findAllDatasets().forEach(dataset -> {
      helper.clearDatasetVariablesCache(dataset);
    });
  }

  public void clearAuthorizationCache() {
    eventBus.post(new SubjectAclUpdatedEvent());
  }

  public void buildDatasetVariablesCache() {
    helper.buildDatasetVariablesCache();
  }

  @Caching(evict = {
    @CacheEvict(value="micaConfig", allEntries = true),
    @CacheEvict(value="aggregations-metadata", allEntries = true),
    @CacheEvict(value = "opal-taxonomies", allEntries = true),
    @CacheEvict(value = "agate-subjects", allEntries = true)
  })
  public void clearAllCaches() {
    log.info("Clearing all caches");
    clearDatasetVariablesCache();
    clearAuthorizationCache();
  }

  @Component
  public static class Helper {

    private static final Logger log = LoggerFactory.getLogger(CacheService.Helper.class);

    @Inject
    private HarmonizationDatasetService harmonizationDatasetService;

    @CacheEvict(value = "dataset-variables", cacheResolver = "datasetVariablesCacheResolver", allEntries = true, beforeInvocation = true)
    public void clearDatasetVariablesCache(Dataset dataset) {
      log.info("Clearing dataset variables cache dataset-{}", dataset.getId());
    }

    @Async
    public void buildDatasetVariablesCache() {
      harmonizationDatasetService.findAllPublishedDatasets().forEach(dataset -> {
        harmonizationDatasetService.getDatasetVariables(dataset).forEach(v -> {
          dataset.getStudyTables().forEach(st -> {
            harmonizationDatasetService
              .getVariableSummary(dataset, v.getName(), st.getStudyId(), st.getProject(), st.getTable());
          });
        });
      });
    }
  }
}
