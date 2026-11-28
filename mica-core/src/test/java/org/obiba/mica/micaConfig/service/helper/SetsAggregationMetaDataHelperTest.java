/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service.helper;

import org.junit.jupiter.api.Test;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider.LocalizedMetaData;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SetsAggregationMetaDataHelperTest {

  private static DocumentSet set(String id, String name) {
    DocumentSet set = new DocumentSet();
    set.setId(id);
    set.setName(name);
    return set;
  }

  @Test
  public void set_titles_are_read_without_loading_the_identifiers() throws Exception {
    VariableSetService variableSetService = mock(VariableSetService.class);
    MicaConfigService micaConfigService = mock(MicaConfigService.class);
    when(micaConfigService.getLocales()).thenReturn(List.of("en"));
    when(micaConfigService.getTranslations("en", false)).thenReturn("{}");
    when(variableSetService.getAllWithoutIdentifiers())
      .thenReturn(List.of(set("s1", "Blood"), set("s2", "dar:Proposal")));

    Map<String, LocalizedMetaData> setIds =
      new VariablesSetsAggregationMetaDataHelper(variableSetService, micaConfigService).getSetIds();

    assertThat(setIds).containsOnlyKeys("s1", "s2");
    assertThat(setIds.get("s1").getTitle().get("en")).isEqualTo("Blood");
    assertThat(setIds.get("s2").getTitle().get("en")).isEqualTo("Proposal");
    verify(variableSetService, never()).getAll();
  }
}
