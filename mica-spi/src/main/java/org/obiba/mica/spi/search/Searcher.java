/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.AdminClient;

import java.util.List;

/**
 * Defines the search operations that can be performed within the search engine.
 *
 */
// TODO isolate elasticsearch as search functionalities
public interface Searcher {

  SearchRequestBuilder prepareSearch(String... indices);

  /**
   * Get field suggestions from query.
   *
   * @param indexName
   * @param type
   * @param limit
   * @param locale
   * @param queryString
   * @param defaultFieldName
   * @return
   */
  List<String> suggest(String indexName, String type, int limit, String locale, String queryString, String defaultFieldName);

}
