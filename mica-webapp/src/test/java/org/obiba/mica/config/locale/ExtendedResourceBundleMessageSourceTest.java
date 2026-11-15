/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config.locale;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.obiba.core.translator.JsonTranslator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The JSON translations are read with JsonPath: a code is a path. Codes can come from the request (error page) or from
 * user values, templates print a translation unescaped (#4765).
 */
public class ExtendedResourceBundleMessageSourceTest {

  private final ExtendedResourceBundleMessageSource messageSource = new ExtendedResourceBundleMessageSource(null, 0) {
    @Override
    JsonTranslator getTranslator(String locale) {
      return new JsonTranslator("{\"http-404\": \"Not Found\", \"global\": {\"name\": \"Name\"}}");
    }
  };

  private String message(String code) {
    // as the templates: the code is the default, the message source returns the code itself when there is no translation
    return messageSource.getMessage(code, null, code, Locale.ENGLISH);
  }

  @Test
  public void test_translation_keys() {
    assertThat(message("http-404")).isEqualTo("Not Found");
    assertThat(message("global.name")).isEqualTo("Name");
    assertThat(message("no-such-code")).isEqualTo("no-such-code");
  }

  @Test
  public void test_json_path_expression_is_not_evaluated() {
    // concat() would return the attacker text, printed as a translation
    String code = "$['http-404'].concat(\"<img src=x onerror=alert(1)>\")";
    assertThat(message(code)).isEqualTo(code);
    assertThat(message("$.global.name")).isEqualTo("$.global.name");
    assertThat(message("global[?(@ =~ /.*/)]")).isEqualTo("global[?(@ =~ /.*/)]");
  }

}
