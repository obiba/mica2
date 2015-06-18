package org.obiba.mica.core.upgrade;

import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.VersionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RuntimeVersionProvider implements VersionProvider {

  private Version version;

  @Autowired
  public RuntimeVersionProvider(@Value("${version}") String version) {
    this.version = new Version(version);
  }

  @Override
  public Version getVersion() {
    return version;
  }
}
