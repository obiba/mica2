/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.util.Locale;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.mica.config.JsonConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.obiba.mica.core.domain.LocalizedString.en;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = { JsonConfiguration.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class LocalizedStringTest {

  private static final Logger log = LoggerFactory.getLogger(LocalizedStringTest.class);

  @Inject
  private ObjectMapper objectMapper;

  @Test
  public void test_localized_string_to_json() throws Exception {
    LocalizedString string = new LocalizedString().forLanguageTag(null, "coucou").forEn("kewkew");
    String json = objectMapper.writeValueAsString(string);
    log.debug(string.toString());
    log.debug(json);
    assertThat(string.keySet().contains(Locale.ENGLISH.toLanguageTag())).isTrue();
    assertThat(string.keySet().contains(Locale.forLanguageTag("und").toLanguageTag())).isTrue();
    LocalizedString string2 = objectMapper.readValue(json, LocalizedString.class);
    log.debug(string2.toString());
    assertThat(string2.keySet().contains(Locale.ENGLISH.toLanguageTag())).isTrue();
    assertThat(string2.keySet().contains(Locale.forLanguageTag("und").toLanguageTag())).isTrue();
  }

  @Test
  public void test_localized_string_as_string() throws Exception {
    LocalizedString string = en("CLSA").forFr("ÉLCV");
    log.debug(string.asUrlSafeString());
    assertThat(string.asUrlSafeString()).isEqualTo("CLSA-ELCV");
  }

  @Test
  public void test_localized_string_as_safe_url() throws Exception {
    LocalizedString string = en("10/66 test").forFr("10/66 test");
    log.debug(string.asUrlSafeString());
    assertThat(string.asUrlSafeString()).isEqualTo("10_66-test");
  }

  @Test
  public void test_localized_string_as_acronym() throws Exception {
    LocalizedString string = en("Canadian Longitudinal Study on Aging")
        .forFr("Étude longitudinale canadienne sur le vieillissement");
    log.debug(string.asAcronym().toString());
    assertThat(string.asAcronym().asUrlSafeString()).isEqualTo("CLSA-E");
  }
}
