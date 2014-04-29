package org.obiba.mica.service;

import javax.inject.Inject;
import javax.validation.Valid;

import org.obiba.mica.domain.MicaConfig;
import org.obiba.mica.event.MicaConfigUpdatedEvent;
import org.obiba.mica.repository.MicaConfigRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

@Component
public class MicaConfigService {

  @Inject
  private MicaConfigRepository micaConfigRepository;

  @Inject
  private EventBus eventBus;

  public MicaConfig getConfig() {
    if(micaConfigRepository.count() == 0) {
      MicaConfig micaConfig = new MicaConfig();
      micaConfig.getLocales().add(MicaConfig.DEFAULT_LOCALE);
      micaConfigRepository.save(micaConfig);
      return getConfig();
    }
    return micaConfigRepository.findAll().get(0);
  }

  public void save(@Valid MicaConfig micaConfig) {
    MicaConfig savedConfig = getConfig();
    BeanUtils.copyProperties(micaConfig, savedConfig, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
        "lastModifiedDate");
    micaConfigRepository.save(savedConfig);
    eventBus.post(new MicaConfigUpdatedEvent(getConfig()));
  }

}
