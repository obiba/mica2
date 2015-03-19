package org.obiba.mica.micaConfig.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

@Component
public class CacheService {

  @CacheEvict(value="opal-taxonomies", allEntries = true)
  public void clearOpalTaxonomiesCache() {
    return;
  }

  @CacheEvict(value="micaConfig", allEntries = true)
  public void clearMicaConfigCache() {
    return;
  }

  @CacheEvict(value="aggregations-metadata", allEntries = true)
  public void clearAggregationsMetadataCache() {
    return;
  }

  @Caching(evict = {
    @CacheEvict(value="micaConfig", allEntries = true),
    @CacheEvict(value="aggregations-metadata", allEntries = true),
    @CacheEvict(value = "opal-taxonomies", allEntries = true),
  })
  public void clearAllCaches() {
    return;
  }
}
