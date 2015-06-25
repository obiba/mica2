package org.obiba.mica.core.upgrade;

import java.util.Optional;

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
    return Optional.ofNullable(micaConfigService.getConfig().getMicaVersion()).orElse(new Version(0, 8));
  }

  @Override
  public void setVersion(Version version) {
    MicaConfig config = micaConfigService.getConfig();
    config.setMicaVersion(version);

    micaConfigService.save(config);
  }
}
