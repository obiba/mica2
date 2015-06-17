package org.obiba.mica.config;

import java.util.ArrayList;
import java.util.List;

import org.obiba.mica.core.upgrade.AttachmentsRefactorUpgrade;
import org.obiba.mica.core.upgrade.RuntimeVersionProvider;
import org.obiba.mica.core.upgrade.MicaVersionModifier;
import org.obiba.runtime.upgrade.UpgradeManager;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.obiba.runtime.upgrade.support.DefaultUpgradeManager;
import org.obiba.runtime.upgrade.support.NullVersionNewInstallationDetectionStrategy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Lists;

@Configuration
public class UpgradeConfiguration {

  @Bean
  public UpgradeManager upgradeManager(List<UpgradeStep> upgradeSteps, MicaVersionModifier micaVersionModifier,
    RuntimeVersionProvider runtimeVersionProvider) {
    DefaultUpgradeManager upgradeManager = new DefaultUpgradeManager();
    upgradeManager.setUpgradeSteps(upgradeSteps);
    upgradeManager.setInstallSteps(new ArrayList<>());
    upgradeManager.setCurrentVersionProvider(micaVersionModifier);
    upgradeManager.setRuntimeVersionProvider(runtimeVersionProvider);

    NullVersionNewInstallationDetectionStrategy newInstallationDetectionStrategy
      = new NullVersionNewInstallationDetectionStrategy();
    newInstallationDetectionStrategy.setVersionProvider(micaVersionModifier);
    upgradeManager.setNewInstallationDetectionStrategy(newInstallationDetectionStrategy);

    return upgradeManager;
  }

  @Bean
  public List<UpgradeStep> upgradeSteps(ApplicationContext applicationContext) {
    return Lists.newArrayList(applicationContext.getBean(AttachmentsRefactorUpgrade.class));
  }
}
