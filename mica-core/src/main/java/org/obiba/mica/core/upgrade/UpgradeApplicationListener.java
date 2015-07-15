package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import org.obiba.runtime.upgrade.UpgradeException;
import org.obiba.runtime.upgrade.UpgradeManager;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.google.common.base.Throwables;

@Component
public class UpgradeApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

  @Inject
  private UpgradeManager upgradeManager;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    try {
      upgradeManager.executeUpgrade();
    } catch(UpgradeException e) {
      throw Throwables.propagate(e);
    }
  }
}
