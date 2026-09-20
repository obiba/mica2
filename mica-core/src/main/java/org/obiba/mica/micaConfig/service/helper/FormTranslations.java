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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.obiba.core.translator.Translator;
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * The texts of a form configuration, stored with it by locale ({@code { "en": { "<key>": "<text>" } }}, the
 * keys being flat and dotted, or nested), looked up before the Mica translations when the keys of the
 * form are resolved.
 */
public final class FormTranslations {

  private static final Logger logger = LoggerFactory.getLogger(FormTranslations.class);

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private FormTranslations() {}

  /**
   * The texts of the form in a locale, by dotted key; empty when the form has none.
   */
  public static Map<String, String> of(EntityConfig config, String locale) {
    Map<String, String> texts = new HashMap<>();
    if (config == null || !config.hasTranslations() || Strings.isNullOrEmpty(locale)) return texts;
    try {
      JsonNode node = objectMapper.readTree(config.getTranslations()).get(locale);
      if (node != null && node.isObject()) flatten(node, "", texts);
    } catch (Exception e) {
      logger.warn("Cannot read the translations of the form configuration: {}", e.getMessage());
    }
    return texts;
  }

  /**
   * A translator resolving the keys from the texts of the form in the locale, else from the fallback
   * (the Mica translations).
   */
  public static Translator translator(EntityConfig config, String locale, Translator fallback) {
    Map<String, String> texts = of(config, locale);
    if (texts.isEmpty()) return fallback;
    return key -> {
      String text = texts.get(key);
      return text != null ? text : fallback.translate(key);
    };
  }

  private static void flatten(JsonNode node, String prefix, Map<String, String> texts) {
    node.fields().forEachRemaining(entry -> {
      String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
      JsonNode value = entry.getValue();
      if (value.isTextual()) texts.put(key, value.asText());
      else if (value.isObject()) flatten(value, key, texts);
    });
  }
}
