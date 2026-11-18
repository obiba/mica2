package org.obiba.mica.config.locale;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.obiba.core.translator.JsonTranslator;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class ExtendedResourceBundleMessageSource extends ReloadableResourceBundleMessageSource {

  /**
   * The JSON translator evaluates the code as a JsonPath expression: a code holding its syntax could build a text
   * (concat()) that templates print as a translation, unescaped, or run costly filters. Codes can come from the request
   * (error page) or from documents and user profiles; translation keys never hold these characters.
   */
  private static final Pattern JSON_PATH_SYNTAX = Pattern.compile("[$\\[\\]()@?*'\"]");

  private final MicaConfigService micaConfigService;

  private Map<String, JsonTranslator> translations = Maps.newConcurrentMap();

  public ExtendedResourceBundleMessageSource(MicaConfigService micaConfigService, int cacheSeconds) {
    this.micaConfigService = micaConfigService;
  }

  @Override
  protected String resolveCodeWithoutArguments(String code, Locale locale) {
    String result = super.resolveCodeWithoutArguments(code, locale);
    if (result != null) return result;
    if (code == null || JSON_PATH_SYNTAX.matcher(code).find()) return null;

    final String key = locale.getLanguage();
    JsonTranslator translator = getTranslator(key);
    if (translator != null) {
      try {
        result = translator.translate(code);
      } catch (Exception e) {
        // ignore
      }
    }

    return result;
  }

  public void evict() {
    translations.clear();
  }

  // package-private for the tests
  JsonTranslator getTranslator(String locale) {
    try {
      if (!translations.containsKey(locale)) {
        String tr = micaConfigService.getTranslations(locale, false);
        if (!Strings.isNullOrEmpty(tr)) translations.put(locale, new JsonTranslator(tr));
      }
      return translations.get(locale);
    } catch (IOException e) {
      // ignore
      return null;
    }
  }

}
