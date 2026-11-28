/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import org.junit.jupiter.api.Test;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AggregationMetaDataResolverTest {

  private static AggregationMetaDataProvider provider(String aggregation, String title) {
    AggregationMetaDataProvider provider = mock(AggregationMetaDataProvider.class);
    when(provider.containsAggregation(aggregation)).thenReturn(true);
    when(provider.getMetadata(anyString(), anyString(), anyString()))
      .thenReturn(AggregationMetaDataProvider.MetaData.newBuilder().title(title).build());
    return provider;
  }

  @Test
  public void first_registered_provider_resolves_a_shared_aggregation() throws Exception {
    // the first provider stands for the cached taxonomy, the others for the costly sets helper
    AggregationMetaDataProvider first = provider("sets", "first");
    List<AggregationMetaDataProvider> others = new ArrayList<>();
    for (int i = 0; i < 20; i++) others.add(provider("sets", "other" + i));
    List<AggregationMetaDataProvider> providers = new ArrayList<>();
    providers.add(first);
    providers.addAll(others);

    AggregationMetaDataResolver resolver = new AggregationMetaDataResolver();
    resolver.afterPropertiesSet();
    resolver.registerProviders(providers);

    for (String term : List.of("s1", "s2", "s3")) {
      assertThat(resolver.getMetaData("sets", term, "en").getTitle()).isEqualTo("first");
    }
    others.forEach(other -> verify(other, never()).getMetadata(anyString(), anyString(), anyString()));
  }
}
