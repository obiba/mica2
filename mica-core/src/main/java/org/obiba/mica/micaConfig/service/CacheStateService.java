/*
 * Copyright (c) 2023 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;


import com.google.common.eventbus.Subscribe;
import org.obiba.mica.micaConfig.event.CacheClearEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class CacheStateService {

  @Inject
  private CacheService cacheService;

  @Async
  @Subscribe
  public void onCacheClear(CacheClearEvent event) {
    switch (event.getName()) {
      case "micaConfig":
      case "mica-config":
        cacheService.clearMicaConfigCache();
        break;
      case "variableTaxonomies":
      case "variable-taxonomies":
        cacheService.clearTaxonomiesCache();
        break;
      case "aggregationsMetadata":
      case "aggregations-metadata":
        cacheService.clearAggregationsMetadataCache();
        break;
      case "datasetVariables":
      case "dataset-variables":
        cacheService.clearDatasetVariablesCache();
        break;
      case "authorization":
        cacheService.clearAuthorizationCache();
        break;
      default:
        cacheService.clearAllCaches();
    }
  }
}
