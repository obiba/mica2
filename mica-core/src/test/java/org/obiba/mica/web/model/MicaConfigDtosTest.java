/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.util.Locale;

import org.junit.Test;
import org.obiba.mica.micaConfig.domain.MicaConfig;

import static org.assertj.core.api.Assertions.assertThat;

public class MicaConfigDtosTest {

  private final MicaConfigDtos dtos = new MicaConfigDtos();

  @Test
  public void test_default_values() {
    MicaConfig config = new MicaConfig();

    Mica.MicaConfigDto dto = dtos.asDto(config);
    assertThat(dto).isNotNull();

    MicaConfig fromDto = dtos.fromDto(dto);
    assertThat(fromDto).isEqualToIgnoringGivenFields(config, "createdDate");
  }

  @Test
  public void test_with_values() {
    MicaConfig config = new MicaConfig();
    config.setName("Test");
    config.setPublicUrl("http://localhost/mica-test");
    config.setDefaultCharacterSet("utf-8");
    config.getLocales().add(Locale.CHINESE);
    config.getLocales().add(Locale.GERMAN);

    Mica.MicaConfigDto dto = dtos.asDto(config);
    assertThat(dto).isNotNull();

    MicaConfig fromDto = dtos.fromDto(dto);
    assertThat(fromDto).isEqualToIgnoringGivenFields(config, "createdDate");
  }
}
