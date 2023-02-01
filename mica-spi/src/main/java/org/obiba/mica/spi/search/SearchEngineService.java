/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search;

import org.obiba.plugins.spi.ServicePlugin;

public interface SearchEngineService extends ServicePlugin {

  /**
   * Provides some Mica configurations useful for the search engine.
   *
   * @param configurationProvider
   */
  void setConfigurationProvider(ConfigurationProvider configurationProvider);

  /**
   * Does the indexing stuff.
   *
   * @return
   */
  Indexer getIndexer();

  /**
   * Does the search stuff.
   *
   * @return
   */
  Searcher getSearcher();

}
