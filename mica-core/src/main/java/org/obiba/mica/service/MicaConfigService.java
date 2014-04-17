package org.obiba.mica.service;

import javax.inject.Inject;

import org.obiba.mica.domain.MicaConfig;
import org.obiba.mica.repository.MicaConfigRepository;
import org.springframework.stereotype.Component;

@Component
public class MicaConfigService {

  @Inject
  private MicaConfigRepository micaConfigRepository;

  public MicaConfig getConfig() {
    if(micaConfigRepository.count() == 0) {
      micaConfigRepository.save(new MicaConfig());
    }
    return micaConfigRepository.findAll().get(0);
  }

}
