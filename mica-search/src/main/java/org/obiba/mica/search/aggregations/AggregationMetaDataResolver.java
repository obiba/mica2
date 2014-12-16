/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class AggregationMetaDataResolver implements AggregationMetaDataProvider {

  @Inject
  private DefaultAggregationMetaDataProvider defaultAggregationTitleProvider;

  private Set<AggregationMetaDataProvider> providers;

  @PostConstruct
  public void init() {
    providers = new HashSet<>();
  }

  @Override
  public MetaData getTitle(String aggregation, String termKey, String locale) {
    Optional<MetaData> title = providers.stream()
      .map(provider -> provider.getTitle(aggregation, termKey, locale)).filter(metaData -> metaData != null)
      .findFirst();

    return title.isPresent() ? title.get() : defaultAggregationTitleProvider.getTitle(aggregation, termKey, locale);
  }

  @Override
  public void refresh() {
    providers.stream().forEach(AggregationMetaDataProvider::refresh);
  }

  public void registerProviders(List<AggregationMetaDataProvider> aggregationMetaDataProviders) {
    providers.addAll(aggregationMetaDataProviders);
  }

  public void unregisterProviders(List<AggregationMetaDataProvider> aggregationMetaDataProviders) {
    providers.removeAll(aggregationMetaDataProviders);
  }

  public void unregisterAllProviders() {
    providers = new HashSet<>();
  }
}
