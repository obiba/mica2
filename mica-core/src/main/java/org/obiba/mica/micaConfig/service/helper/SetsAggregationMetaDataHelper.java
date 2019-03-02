package org.obiba.mica.micaConfig.service.helper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider.LocalizedMetaData;
import org.springframework.stereotype.Component;

@Component
public class SetsAggregationMetaDataHelper extends AbstractIdAggregationMetaDataHelper {

  private VariableSetService variableSetService;

  private MicaConfigService micaConfigService;

  @Inject
  public SetsAggregationMetaDataHelper(
    VariableSetService variableSetService,
    MicaConfigService micaConfigService) {
    this.variableSetService = variableSetService;
    this.micaConfigService = micaConfigService;
  }

  public Map<String, LocalizedMetaData> getSetIds() {
    List<String> locales = micaConfigService.getLocales();

    return variableSetService.getAll().stream()
      .collect(Collectors.toMap(
        DocumentSet::getId,
        set -> {
          LocalizedString title = new LocalizedString();
          locales.forEach(locale -> title.forLanguageTag(locale, set.getName()));
          return new LocalizedMetaData(title, new LocalizedString(), set.getType());
        }
      ));
  }

  @Override
  protected Map<String, LocalizedMetaData> getIdAggregationMap() {
    return getSetIds();
  }
}
