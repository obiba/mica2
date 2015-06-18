package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.VersionModifier;
import org.springframework.stereotype.Component;

@Component
public class MicaVersionModifier implements VersionModifier {

  @Inject
  private MicaConfigService micaConfigService;

  @Override
  public Version getVersion() {
    return micaConfigService.getConfig().getMicaVersion();
  }

  @Override
  public void setVersion(Version version) {
    MicaConfig config = micaConfigService.getConfig();
    config.setMicaVersion(version);

    micaConfigService.save(config);
  }
}
