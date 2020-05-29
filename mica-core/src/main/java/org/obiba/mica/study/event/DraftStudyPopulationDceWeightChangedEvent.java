package org.obiba.mica.study.event;

import org.obiba.mica.core.event.PersistableUpdatedEvent;
import org.obiba.mica.study.domain.BaseStudy;

public class DraftStudyPopulationDceWeightChangedEvent extends PersistableUpdatedEvent<BaseStudy>  {

  public DraftStudyPopulationDceWeightChangedEvent(BaseStudy study) {
    super(study);
  }
}
