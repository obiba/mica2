/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.upgrade;

import jakarta.inject.Inject;

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
