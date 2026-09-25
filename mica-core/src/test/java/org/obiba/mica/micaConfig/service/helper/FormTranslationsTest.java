package org.obiba.mica.micaConfig.service.helper;

import org.junit.jupiter.api.Test;
import org.obiba.core.translator.PrefixedValueTranslator;
import org.obiba.core.translator.Translator;
import org.obiba.mica.micaConfig.domain.NetworkConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class FormTranslationsTest {

  private final Translator bundle = key -> "website".equals(key) ? "Website" : key;

  @Test
  public void form_texts_come_first_then_the_bundle() {
    NetworkConfig config = new NetworkConfig();
    config.setTranslations("{\"en\": {\"phone.title\": \"Phone\", \"nested\": {\"hint\": \"Digits\"}}, \"fr\": {\"phone.title\": \"Téléphone\"}}");

    Translator en = FormTranslations.translator(config, "en", bundle);
    assertEquals("Phone", en.translate("phone.title"));
    assertEquals("Digits", en.translate("nested.hint"));
    assertEquals("Website", en.translate("website"));
    assertEquals("unknown", en.translate("unknown"));

    assertEquals("Téléphone", FormTranslations.translator(config, "fr", bundle).translate("phone.title"));
    Translator de = FormTranslations.translator(config, "de", bundle);
    assertEquals("Phone", de.translate("phone.title"));
    assertEquals("Website", de.translate("website"));
    assertEquals("Digits", FormTranslations.translator(config, "fr", bundle).translate("nested.hint"));

    assertEquals("<h3>Phone</h3>", new PrefixedValueTranslator(en).translate("<h3>t(phone.title)</h3>"));
  }

  @Test
  public void no_translations_means_the_bundle() {
    NetworkConfig config = new NetworkConfig();
    assertSame(bundle, FormTranslations.translator(config, "en", bundle));
    config.setTranslations("not json");
    assertSame(bundle, FormTranslations.translator(config, "en", bundle));
  }
}
