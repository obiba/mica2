/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.service;

import java.util.List;

import javax.annotation.Nullable;

import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.network.domain.Network;

import com.google.common.collect.Lists;

public interface PublishedNetworkService extends PublishedDocumentService<Network> {

  /**
   * Get the network list in a object that informs about the set of networks that is retrieved.
   *
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @param studyId
   * @return
   */
  Networks getNetworks(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String studyId);

  /**
   * @{link Network} query result container.
   */
  class Networks {
    private final int total;

    private final int from;

    private final int limit;

    private final List<Network> list = Lists.newArrayList();

    public Networks(int total, int from, int limit) {
      this.total = total;
      this.from = from;
      this.limit = limit;
    }

    public List<Network> getList() {
      return list;
    }

    public void add(Network network) {
      if(network != null) list.add(network);
    }

    public int getTotal() {
      return total;
    }

    public int getFrom() {
      return from;
    }

    public int getLimit() {
      return limit;
    }
  }

}
