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
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider.LocalizedMetaData;
import org.obiba.mica.micaConfig.service.helper.SetsAggregationMetaDataHelper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SetsAggregationMetaDataProviderTest {

  private static LocalizedMetaData metaData(String title) {
    return new LocalizedMetaData(new LocalizedString().forLanguageTag("en", title), new LocalizedString(), "Set");
  }

  private static SetsAggregationMetaDataProvider provider(SetsAggregationMetaDataHelper helper) {
    return new SetsAggregationMetaDataProvider(helper) {};
  }

  @Test
  public void set_titles_are_read_once_per_query() {
    SetsAggregationMetaDataHelper helper = mock(SetsAggregationMetaDataHelper.class);
    when(helper.getSetIds()).thenReturn(Map.of("s1", metaData("Set 1"), "s2", metaData("Set 2")));
    SetsAggregationMetaDataProvider provider = provider(helper);

    provider.refresh();
    for (String term : List.of("s1", "s2", "s3")) provider.getMetadata("sets", term, "en");
    verify(helper, times(1)).getSetIds();

    // next query reads them again
    provider.refresh();
    provider.getMetadata("sets", "s1", "en");
    verify(helper, times(2)).getSetIds();
  }

  @Test
  public void resolves_known_sets_only() {
    SetsAggregationMetaDataHelper helper = mock(SetsAggregationMetaDataHelper.class);
    when(helper.getSetIds()).thenReturn(Map.of("s1", metaData("Set 1")));
    SetsAggregationMetaDataProvider provider = provider(helper);
    provider.refresh();

    assertThat(provider.getMetadata("sets", "s1", "en").getTitle()).isEqualTo("Set 1");
    assertThat(provider.getMetadata("sets", "unknown", "en")).isNull();
  }

  @Test
  public void other_aggregations_do_not_read_the_sets() {
    SetsAggregationMetaDataHelper helper = mock(SetsAggregationMetaDataHelper.class);
    SetsAggregationMetaDataProvider provider = provider(helper);
    provider.refresh();

    assertThat(provider.getMetadata("studyId", "s1", "en")).isNull();
    verify(helper, never()).getSetIds();
  }
}
