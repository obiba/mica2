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

import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.PrefixedValueTranslator;
import org.obiba.core.translator.TranslationUtils;
import org.obiba.core.translator.Translator;
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;

/**
 * Schema form settings.
 */
/**
 * The form of a data access entity as rendered by the portal: the {@code t()} tokens of the schema and of the
 * definition are resolved from the texts stored with the form first, then from the Mica translations.
 */
public class SchemaFormConfig {

  private final String schema;

  private final String definition;

  private final String model;

  private final boolean readOnly;

  public SchemaFormConfig(MicaConfigService micaConfigService, EntityConfig form, String model, String lang, boolean readOnly) {
    this.readOnly = readOnly;
    Translator bundle = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(lang, false));
    Translator translator = new PrefixedValueTranslator(FormTranslations.translator(form, lang, bundle));
    TranslationUtils translationUtils = new TranslationUtils();
    this.schema = translationUtils.translate(form.getSchema(), translator).replaceAll("col-xs-", "col-");
    this.definition = translationUtils.translate(form.getDefinition(), translator).replaceAll("col-xs-", "col-");
    this.model = model;
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
