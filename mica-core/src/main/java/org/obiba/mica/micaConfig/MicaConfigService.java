package org.obiba.mica.micaConfig;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.eventbus.EventBus;

@Service
@Validated
public class MicaConfigService {

  @Inject
  private MicaConfigRepository micaConfigRepository;

  @Inject
  private EventBus eventBus;

  @Cacheable(value = "micaConfig", key = "#root.methodName")
  public MicaConfig getConfig() {
    return getOrCreateMicaConfig();
  }

  private MicaConfig getOrCreateMicaConfig() {
    if(micaConfigRepository.count() == 0) {
      MicaConfig micaConfig = new MicaConfig();
      micaConfig.getLocales().add(MicaConfig.DEFAULT_LOCALE);
      micaConfigRepository.save(micaConfig);
      return getConfig();
    }
    return micaConfigRepository.findAll().get(0);
  }

  @CacheEvict(value = "micaConfig", allEntries = true)
  public void save(@NotNull @Valid MicaConfig micaConfig) {
    MicaConfig savedConfig = getOrCreateMicaConfig();
    BeanUtils.copyProperties(micaConfig, savedConfig, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
        "lastModifiedDate");
    micaConfigRepository.save(savedConfig);
    eventBus.post(new MicaConfigUpdatedEvent(getConfig()));
  }

}
