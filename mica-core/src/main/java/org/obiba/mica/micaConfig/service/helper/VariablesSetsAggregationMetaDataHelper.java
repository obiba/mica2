package org.obiba.mica.micaConfig.service.helper;

import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class VariablesSetsAggregationMetaDataHelper extends SetsAggregationMetaDataHelper {

  private final VariableSetService variableSetService;

  @Inject
  public VariablesSetsAggregationMetaDataHelper(
    VariableSetService variableSetService,
    MicaConfigService micaConfigService) {
    super(micaConfigService);
    this.variableSetService = variableSetService;
  }

  @Override
  protected VariableSetService getDocumentSetService() {
    return variableSetService;
  }

  @Override
  protected String getDocumentSetName(DocumentSet set) {
    return set.getName().startsWith("dar:") ? set.getName().replace("dar:", "") : set.getName();
  }
}
