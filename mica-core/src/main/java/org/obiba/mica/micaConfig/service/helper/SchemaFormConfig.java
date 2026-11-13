/*
 * Copyright (c) 2024 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.PrefixedValueTranslator;
import org.obiba.core.translator.TranslationUtils;
import org.obiba.core.translator.Translator;
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;

/**
 * The form of a data access entity as rendered by the portal: the {@code t()} tokens of the schema and of the
 * definition are resolved from the texts stored with the form first, then from the Mica translations.
 */
public class SchemaFormConfig {

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

  private final String schema;

  private final String definition;

  private final String model;

  private final boolean readOnly;

  public SchemaFormConfig(MicaConfigService micaConfigService, EntityConfig form, String model, String lang, boolean readOnly) {
    this.readOnly = readOnly;
    Translator bundle = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(lang, false));
    Translator translator = new PrefixedValueTranslator(FormTranslations.translator(form, lang, bundle));
    TranslationUtils translationUtils = new TranslationUtils();
    this.schema = scriptSafe(translationUtils.translate(form.getSchema(), translator).replaceAll("col-xs-", "col-"));
    this.definition = scriptSafe(translationUtils.translate(form.getDefinition(), translator).replaceAll("col-xs-", "col-"));
    this.model = scriptSafe(jsonObject(model));
  }

  /**
   * The page prints the schema, the definition and the model as is in a script block. In JSON a "&lt;" is only found
   * in a string: written as a unicode escape it is the same JSON, and no text typed in a form (or in a translation)
   * can end the script block (<code>&lt;/script&gt;</code>) or start a comment (<code>&lt;!--</code>).
   *
   * @param json
   * @return the same JSON without any "&lt;" character, null if the JSON is null
   */
  static String scriptSafe(String json) {
    if (json == null) return null;
    return json.replace("<", "\\u003c").replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
  }

  /**
   * The content of a data access entity is saved as received: when it is not a JSON object it would be printed in
   * the page as script, an empty model is used instead.
   *
   * @param content
   * @return the content if it is a JSON object, an empty object otherwise, null if the content is null or empty
   */
  static String jsonObject(String content) {
    if (Strings.isNullOrEmpty(content)) return null;
    try {
      // trailing tokens are an error: {} followed by anything is not a JSON object
      JsonNode node = JSON_MAPPER.readTree(content);
      return node != null && node.isObject() ? content : "{}";
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }

  public String getSchema() {
    return schema;
  }

  public String getDefinition() {
    return definition;
  }

  public String getModel() {
    return model;
  }

  public boolean isReadOnly() {
    return readOnly;
  }
}
