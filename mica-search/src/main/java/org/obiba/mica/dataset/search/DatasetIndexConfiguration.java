/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import java.io.IOException;

import com.google.common.collect.Lists;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.obiba.mica.core.domain.TaxonomyTarget;
import org.obiba.mica.search.AbstractIndexConfiguration;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.stereotype.Component;

@Component
public class DatasetIndexConfiguration extends AbstractIndexConfiguration
  implements ElasticSearchIndexer.IndexConfigurationListener {

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(DatasetIndexer.DRAFT_DATASET_INDEX.equals(indexName) ||
      DatasetIndexer.PUBLISHED_DATASET_INDEX.equals(indexName)) {

      try {
        client.admin().indices().preparePutMapping(indexName).setType(DatasetIndexer.DATASET_TYPE)
          .setSource(createMappingProperties()).execute().actionGet();
      } catch(IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private XContentBuilder createMappingProperties() throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(DatasetIndexer.DATASET_TYPE);
    mapping.startObject("properties");
    Taxonomy taxonomy = getTaxonomy();
    addStaticVocabularies(taxonomy, "studyTable.id", "studyTable.studyId", //
      "studyTables.id", "studyTables.studyId");
    addLocalizedVocabularies(taxonomy, "name", "acronym", "description");
    addTaxonomyFields(mapping, taxonomy, Lists.newArrayList());
    mapping.endObject().endObject();

    return mapping;
  }

  @Override
  protected TaxonomyTarget getTarget() {
    return TaxonomyTarget.DATASET;
  }
}
