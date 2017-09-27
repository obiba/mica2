/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.obiba.mica.micaConfig.service.PluginsService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class SearchEngineClient {

  @Inject
  private PluginsService pluginsService;

  private Client getClient() {
    return pluginsService.getSearchEngineService().getClient();
  }

  public SearchRequestBuilder prepareSearch(String... indices) {
    return getClient().prepareSearch(indices);
  }

  public AdminClient admin() {
    return getClient().admin();
  }

}
