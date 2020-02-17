package org.obiba.mica.web.controller.domain;

import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.PrefixedValueTranslator;
import org.obiba.core.translator.TranslationUtils;
import org.obiba.core.translator.Translator;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.web.controller.DataAccessController;

/**
 * Schema form settings.
 */
public class SchemaFormConfig {

  private MicaConfigService micaConfigService;

  private final String schema;
  private final String definition;
  private final String model;
  private final boolean readOnly;

  public SchemaFormConfig(MicaConfigService micaConfigService, String schema, String definition, String model, String locale, boolean readOnly) {
    this.micaConfigService = micaConfigService;
    this.readOnly = readOnly;
    String lang = locale == null ? "en" : locale.replaceAll("\"", "");
    Translator translator = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(lang, false));
    translator = new PrefixedValueTranslator(translator);

    TranslationUtils translationUtils = new TranslationUtils();
    this.schema = translationUtils.translate(schema, translator).replaceAll("col-xs-", "col-");
    this.definition = translationUtils.translate(definition, translator).replaceAll("col-xs-", "col-");
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
