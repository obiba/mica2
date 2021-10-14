package org.obiba.mica.micaConfig.service.helper;

import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.study.service.StudySetService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class StudiesSetsAggregationMetaDataHelper extends SetsAggregationMetaDataHelper {

  private final StudySetService studySetService;

  @Inject
  public StudiesSetsAggregationMetaDataHelper(
    StudySetService studySetService,
    MicaConfigService micaConfigService) {
    super(micaConfigService);
    this.studySetService = studySetService;
  }

  @Override
  protected StudySetService getDocumentSetService() {
    return studySetService;
  }
}
