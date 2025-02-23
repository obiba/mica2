/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Scope("prototype")
public class AggregationMetaDataResolver implements InitializingBean {

  @Inject
  private DefaultAggregationMetaDataProvider defaultAggregationTitleProvider;

  private Set<AggregationMetaDataProvider> providers;

  private Map<String, AggregationMetaDataProvider> aggregationProviderMap;

  @Override
  public void afterPropertiesSet() throws Exception {
    providers = new HashSet<>();
    aggregationProviderMap = Maps.newHashMap();
  }

  public AggregationMetaDataProvider.MetaData getMetaData(String aggregation, String termKey, String locale) {
    if (!aggregationProviderMap.containsKey(aggregation)) {
      boolean found = false;

      for (AggregationMetaDataProvider provider: providers) {
        if (provider.containsAggregation(aggregation)) {
          aggregationProviderMap.put(aggregation, provider);
          found = true;
          break;
        }
      }

      if (!found) aggregationProviderMap.put(aggregation, null);
    }

    AggregationMetaDataProvider provider = aggregationProviderMap.get(aggregation);

    if(provider == null)
      return defaultAggregationTitleProvider.getMetadata(aggregation, termKey, locale);

    AggregationMetaDataProvider.MetaData md = provider.getMetadata(aggregation, termKey, locale);

    return md != null ? md : defaultAggregationTitleProvider.getMetadata(aggregation, termKey, locale);
  }

  public void refresh() {
    aggregationProviderMap = Maps.newHashMap();
    providers.forEach(AggregationMetaDataProvider::refresh);
  }

  public void registerProviders(List<AggregationMetaDataProvider> aggregationMetaDataProviders) {
    providers.addAll(aggregationMetaDataProviders);
  }

  public void unregisterProviders(List<AggregationMetaDataProvider> aggregationMetaDataProviders) {
    providers.removeAll(aggregationMetaDataProviders);
  }

  public void unregisterAllProviders() {
    providers = Sets.newHashSet();
  }
}
