package org.obiba.mica.micaConfig.service.helper;

import com.google.common.base.Strings;
import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.Translator;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.service.DocumentSetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider.LocalizedMetaData;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class SetsAggregationMetaDataHelper extends AbstractIdAggregationMetaDataHelper {

  private final MicaConfigService micaConfigService;

  protected SetsAggregationMetaDataHelper(
    MicaConfigService micaConfigService) {
    this.micaConfigService = micaConfigService;
  }

  protected abstract DocumentSetService getDocumentSetService();

  public Map<String, LocalizedMetaData> getSetIds() {
    List<String> locales = micaConfigService.getLocales();

    Map<String, Translator> translators = locales.stream()
      .collect(Collectors.toMap(Function.identity(), locale -> JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(locale, false))));

    return getDocumentSetService().getAll().stream()
      .collect(Collectors.toMap(
        DocumentSet::getId,
        set -> {
          LocalizedString title = new LocalizedString();
          locales.forEach(locale -> {
            String setName = Strings.isNullOrEmpty(set.getName()) ?
              translators.get(locale).translate("sets.cart.title") :
              set.getName();
            title.forLanguageTag(locale, setName);
          });
          return new LocalizedMetaData(title, new LocalizedString(), set.getType());
        }
      ));
  }

  @Override
  protected Map<String, LocalizedMetaData> getIdAggregationMap() {
    return getSetIds();
  }
}
