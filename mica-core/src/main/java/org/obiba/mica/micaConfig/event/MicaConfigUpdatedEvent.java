/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.event;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.obiba.mica.core.event.PersistableUpdatedEvent;
import org.obiba.mica.micaConfig.domain.MicaConfig;

import com.google.common.collect.Lists;

public class MicaConfigUpdatedEvent extends PersistableUpdatedEvent<MicaConfig> {

  private List<String> removedRoles = Lists.newArrayList();

  public MicaConfigUpdatedEvent(MicaConfig micaConfig, @NotNull List<String> removedRoles) {
    super(micaConfig);
    this.removedRoles = removedRoles;
  }

  public List<String> getRemovedRoles() {
    return removedRoles;
  }
}
