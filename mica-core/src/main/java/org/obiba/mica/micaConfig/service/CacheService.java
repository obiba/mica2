package org.obiba.mica.micaConfig.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class CacheService {

  @CacheEvict(value="opal-taxonomies", allEntries = true)
  public void clearCache() {
    return;
  }
}
