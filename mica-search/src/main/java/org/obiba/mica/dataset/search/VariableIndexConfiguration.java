/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import org.elasticsearch.client.Client;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.springframework.stereotype.Component;

@Component
public class VariableIndexConfiguration implements ElasticSearchIndexer.IndexConfigurationListener {

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(VariableIndexer.DRAFT_VARIABLE_INDEX.equals(indexName) ||
        VariableIndexer.PUBLISHED_VARIABLE_INDEX.equals(indexName)) {
      setVariableParentType(client, indexName);
    }
  }

  private void setVariableParentType(Client client, String indexName) {
    client.admin().indices().preparePutMapping(indexName).setType(VariableIndexer.HARMONIZED_VARIABLE_TYPE)
        .setSource("_parent", "type=" + VariableIndexer.VARIABLE_TYPE).execute().actionGet();
  }
}
