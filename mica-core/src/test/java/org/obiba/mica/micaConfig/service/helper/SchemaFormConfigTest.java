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
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.MicaConfigService;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SchemaFormConfigTest {

  private static final String BUNDLE_EN = "{\"data-access-request\": {\"default\": {\"name\": \"Bundle name\", \"help\": \"Bundle help\"}}}";

  private static final String BUNDLE_FR = "{\"data-access-request\": {\"default\": {\"name\": \"Nom (bundle)\", \"help\": \"Aide (bundle)\"}}}";

  private static final String SCHEMA = "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\", \"title\": \"t(data-access-request.default.name)\"}}}";

  private static final String UISCHEMA = "{\"type\": \"VerticalLayout\", \"elements\": [{\"type\": \"Label\", \"text\": \"<p class='col-xs-6'>t(data-access-request.default.help)</p>\"}, {\"type\": \"Control\", \"scope\": \"#/properties/name\"}]}";

  private MicaConfigService micaConfigService() throws Exception {
    MicaConfigService micaConfigService = mock(MicaConfigService.class);
    when(micaConfigService.getTranslations(eq("en"), anyBoolean())).thenReturn(BUNDLE_EN);
    when(micaConfigService.getTranslations(eq("fr"), anyBoolean())).thenReturn(BUNDLE_FR);
    return micaConfigService;
  }

  @Test
  public void form_texts_come_first_then_the_bundle() throws Exception {
    DataAccessForm form = new DataAccessForm();
    form.setSchema(SCHEMA);
    form.setDefinition(UISCHEMA);
    form.setTranslations("{\"en\": {\"data-access-request.default.name\": \"Form name\"}, \"fr\": {\"data-access-request.default.name\": \"Nom (formulaire)\"}}");

    SchemaFormConfig en = new SchemaFormConfig(micaConfigService(), form, "{\"name\": \"Jane\"}", "en", true);
    assertTrue(en.getSchema().contains("\"title\":\"Form name\""), en.getSchema());
    assertTrue(en.getDefinition().contains("Bundle help"), en.getDefinition());
    assertTrue(en.getDefinition().contains("class='col-6'"), en.getDefinition());
    assertEquals("{\"name\": \"Jane\"}", en.getModel());
    assertTrue(en.isReadOnly());

    SchemaFormConfig fr = new SchemaFormConfig(micaConfigService(), form, "{}", "fr", false);
    assertTrue(fr.getSchema().contains("\"title\":\"Nom (formulaire)\""), fr.getSchema());
    assertTrue(fr.getDefinition().contains("Aide (bundle)"), fr.getDefinition());
  }

  @Test
  public void values_cannot_end_the_script_block_of_the_page() throws Exception {
    DataAccessForm form = new DataAccessForm();
    form.setSchema(SCHEMA);
    form.setDefinition(UISCHEMA);
    form.setTranslations("{\"en\": {\"data-access-request.default.name\": \"</script><img src=x onerror=alert(1)>\"}}");

    String model = "{\"name\": \"a</script><img src=x onerror=alert(2)><!-- b\"}";
    SchemaFormConfig config = new SchemaFormConfig(micaConfigService(), form, model, "en", true);
    for (String json : new String[] { config.getSchema(), config.getDefinition(), config.getModel() }) {
      assertFalse(json.contains("<"), json);
    }
    // same JSON
    assertEquals(new ObjectMapper().readTree(model), new ObjectMapper().readTree(config.getModel()));
    assertTrue(config.getDefinition().contains("\\u003cp class='col-6'>"), config.getDefinition());
  }

  @Test
  public void content_that_is_not_a_json_object_is_not_printed() throws Exception {
    DataAccessForm form = new DataAccessForm();
    form.setSchema(SCHEMA);
    form.setDefinition(UISCHEMA);

    for (String content : new String[] { "{}; alert(1); //", "alert(1)", "[1]", "\"text\"", "1", "{\"a\": 1} alert(1)", "{" }) {
      assertEquals("{}", new SchemaFormConfig(micaConfigService(), form, content, "en", true).getModel(), content);
    }
    assertNull(new SchemaFormConfig(micaConfigService(), form, null, "en", true).getModel());
    assertNull(new SchemaFormConfig(micaConfigService(), form, "", "en", true).getModel());
    assertEquals("{\"a\": {\"b\": [1, \"c\"]}}", new SchemaFormConfig(micaConfigService(), form, "{\"a\": {\"b\": [1, \"c\"]}}", "en", true).getModel());
  }

  @Test
  public void no_form_texts_means_the_bundle() throws Exception {
    DataAccessForm form = new DataAccessForm();
    form.setSchema(SCHEMA);
    form.setDefinition(UISCHEMA);

    SchemaFormConfig en = new SchemaFormConfig(micaConfigService(), form, "{}", "en", false);
    assertTrue(en.getSchema().contains("\"title\":\"Bundle name\""), en.getSchema());
    assertTrue(en.getDefinition().contains("Bundle help"), en.getDefinition());
  }
}
