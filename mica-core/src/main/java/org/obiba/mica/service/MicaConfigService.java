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

  // cache to avoid MongoDB round trip
  private MicaConfig cachedConfig;

  public MicaConfig getConfig() {
    if(cachedConfig != null) {
      return cachedConfig;
    }
    if(micaConfigRepository.count() == 0) {
      MicaConfig micaConfig = new MicaConfig();
      micaConfig.getLocales().add(MicaConfig.DEFAULT_LOCALE);
      micaConfigRepository.save(micaConfig);
      return getConfig();
    }
    cachedConfig = micaConfigRepository.findAll().get(0);
    return cachedConfig;
  }

  public void save(@Valid MicaConfig micaConfig) {
    MicaConfig savedConfig = getConfig();
    BeanUtils.copyProperties(micaConfig, savedConfig, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
        "lastModifiedDate");
    micaConfigRepository.save(savedConfig);
    cachedConfig = null;
    eventBus.post(new MicaConfigUpdatedEvent(getConfig()));
  }

}
