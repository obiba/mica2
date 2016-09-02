/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import com.google.common.base.MoreObjects;

import java.io.Serializable;

/**
 * Represents a opal {@link Table} that is associated to a {@link org.obiba.mica.network.domain.Network}.
 */
public class NetworkTable extends Table implements Serializable {

  private static final long serialVersionUID = -8902121703886344210L;

  private String networkId;

  public String getNetworkId() {
    return networkId;
  }

  public void setNetworkId(String networkId) {
    this.networkId = networkId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("project", getProject()).add("table", getTable())
        .add("networkId", networkId).toString();
  }
}
