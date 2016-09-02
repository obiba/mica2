package org.obiba.mica.config;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.obiba.mica.core.upgrade.*;
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

  @Resource(name="upgradeSteps")
  List<UpgradeStep> upgradeSteps;

  @Bean
  public UpgradeManager upgradeManager(MicaVersionModifier micaVersionModifier,
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

  @Bean(name = "upgradeSteps")
  public List<UpgradeStep> upgradeSteps(ApplicationContext applicationContext) {
    return Lists.newArrayList(applicationContext.getBean(AttachmentsRefactorUpgrade.class), //
      applicationContext.getBean(NetworkLogoMigration.class), //
      applicationContext.getBean(NetworkStateUpgrade.class), //
      applicationContext.getBean(AttachmentsMigration.class), //
      applicationContext.getBean(AttachmentsPathUpgrade.class), //
      applicationContext.getBean(AttachmentsCleanupUpgrade.class), //
      applicationContext.getBean(ContactsRefactorUpgrade.class), //
      applicationContext.getBean(DatasetStateUpgrade.class), //
      applicationContext.getBean(ElasticsearchUpgrade.class));
  }
}
