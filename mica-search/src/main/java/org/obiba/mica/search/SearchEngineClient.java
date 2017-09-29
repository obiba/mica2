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
import org.obiba.mica.micaConfig.service.PluginsService;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class SearchEngineClient implements Searcher {

  @Inject
  private PluginsService pluginsService;

  private Searcher getSearcher() {
    return pluginsService.getSearchEngineService().getSearcher();
  }

  @Override
  public SearchRequestBuilder prepareSearch(String... indices) {
    return getSearcher().prepareSearch(indices);
  }

}
