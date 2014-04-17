package org.obiba.mica.service;

import javax.inject.Inject;

import org.obiba.mica.domain.MicaConfig;
import org.obiba.mica.repository.MicaConfigRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class MicaConfigService {

  @Inject
  private MicaConfigRepository micaConfigRepository;

  //TODO use cache to avoid mongoDB round trip
  public MicaConfig getConfig() {
    if(micaConfigRepository.count() == 0) {
      MicaConfig micaConfig = new MicaConfig();
      micaConfig.getLocales().add(MicaConfig.DEFAULT_LOCALE);
      micaConfigRepository.save(micaConfig);
    }
    return micaConfigRepository.findAll().get(0);
  }

  public void save(MicaConfig micaConfig) {
    MicaConfig savedConfig = getConfig();
    BeanUtils.copyProperties(micaConfig, savedConfig, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
        "lastModifiedDate");
    micaConfigRepository.save(savedConfig);
  }

}
