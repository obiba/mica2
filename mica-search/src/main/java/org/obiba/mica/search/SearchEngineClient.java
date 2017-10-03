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
import org.obiba.mica.spi.search.support.JoinQuery;
import org.obiba.mica.spi.search.support.Query;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.InputStream;
import java.util.List;

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

  @Override
  public JoinQuery makeJoinQuery(String rql) {
    return getSearcher().makeJoinQuery(rql);
  }

  @Override
  public Query makeQuery(String rql) {
    return getSearcher().makeQuery(rql);
  }

  @Override
  public DocumentResults find(String indexName, String type, String rql) {
    return getSearcher().find(indexName, type, rql);
  }

  @Override
  public List<String> suggest(String indexName, String type, int limit, String locale, String queryString, String defaultFieldName) {
    return getSearcher().suggest(indexName, type, limit, locale, queryString, defaultFieldName);
  }

  @Override
  public InputStream getDocumentById(String indexName, String type, String id) {
    return getSearcher().getDocumentById(indexName, type, id);
  }

  @Override
  public InputStream getDocumentByClassName(String indexName, String type, Class clazz, String id) {
    return getSearcher().getDocumentByClassName(indexName, type, clazz, id);
  }

  @Override
  public DocumentResults getDocumentsByClassName(String indexName, String type, Class clazz, int from, int limit, String sort, String order, String queryString, TermFilter termFilter, IdFilter idFilter) {
    return getSearcher().getDocumentsByClassName(indexName, type, clazz, from, limit, sort, order, queryString, termFilter, idFilter);
  }

  @Override
  public DocumentResults getDocuments(String indexName, String type, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable TermFilter termFilter, @Nullable IdFilter idFilter, @Nullable List<String> fields, @Nullable List<String> excludedFields) {
    return getSearcher().getDocuments(indexName, type, from, limit, sort, order, queryString, termFilter, idFilter, fields, excludedFields);
  }

  @Override
  public long countDocumentsWithField(String indexName, String type, String field) {
    return getSearcher().countDocumentsWithField(indexName, type, field);
  }
}
