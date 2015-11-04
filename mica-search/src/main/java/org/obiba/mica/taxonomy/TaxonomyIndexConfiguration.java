/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy;

import java.io.IOException;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.obiba.mica.search.AbstractIndexConfiguration;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TaxonomyIndexConfiguration extends AbstractIndexConfiguration implements ElasticSearchIndexer.IndexConfigurationListener {

  private static final Logger log = LoggerFactory.getLogger(TaxonomyIndexConfiguration.class);

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(TaxonomyIndexer.TAXONOMY_INDEX.equals(indexName)) {
      try {
        client.admin().indices().preparePutMapping(indexName).setType(TaxonomyIndexer.TAXONOMY_TERM_TYPE)
          .setSource(createMappingProperties(TaxonomyIndexer.TAXONOMY_TERM_TYPE)).execute().actionGet();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private XContentBuilder createMappingProperties(String type) throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(type);
    mapping.startObject("properties");
    mapping.startObject("id").field("type", "string").field("index", "not_analyzed").endObject();
    createMappingWithAndWithoutAnalyzer(mapping, "name");
    createMappingWithAndWithoutAnalyzer(mapping, "taxonomyName");
    createMappingWithAndWithoutAnalyzer(mapping, "vocabularyName");
    mapping.endObject(); // properties
    mapping.endObject().endObject();
    return mapping;
  }
}
