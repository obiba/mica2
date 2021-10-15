package org.obiba.mica.micaConfig.service.helper;

import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.NetworkSetService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class NetworksSetsAggregationMetaDataHelper extends SetsAggregationMetaDataHelper {

  private final NetworkSetService networkSetService;

  @Inject
  public NetworksSetsAggregationMetaDataHelper(
    NetworkSetService networkSetService,
    MicaConfigService micaConfigService) {
    super(micaConfigService);
    this.networkSetService = networkSetService;
  }

  @Override
  protected NetworkSetService getDocumentSetService() {
    return networkSetService;
  }
}
