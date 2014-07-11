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
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.search.StudyIndexer;
import org.springframework.stereotype.Component;

@Component
public class DatasetIndexConfiguration implements ElasticSearchIndexer.IndexConfigurationListener {

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(DatasetIndexer.DRAFT_DATASET_INDEX.equals(indexName) ||
        DatasetIndexer.PUBLISHED_DATASET_INDEX.equals(indexName)) {
      setVariableParentType(client, indexName, Dataset.MAPPING_NAME);
    }

    if(StudyIndexer.DRAFT_STUDY_INDEX.equals(indexName) ||
        StudyIndexer.PUBLISHED_STUDY_INDEX.equals(indexName)) {
      setVariableParentType(client, indexName, Study.class.getSimpleName());
    }
  }

  private void setVariableParentType(Client client, String indexName, String parent) {
    client.admin().indices().preparePutMapping(indexName).setType(DatasetVariable.MAPPING_NAME)
        .setSource("_parent", "type=" + parent).execute().actionGet();
  }
}
