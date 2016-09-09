/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy;

import java.io.IOException;
import java.util.stream.Stream;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.obiba.mica.search.AbstractIndexConfiguration;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TaxonomyIndexConfiguration extends AbstractIndexConfiguration
  implements ElasticSearchIndexer.IndexConfigurationListener {

  private static final Logger log = LoggerFactory.getLogger(TaxonomyIndexConfiguration.class);

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(TaxonomyIndexer.TAXONOMY_INDEX.equals(indexName)) {
      try {
        client.admin().indices().preparePutMapping(indexName) //
          .setType(TaxonomyIndexer.TAXONOMY_TYPE).setSource(createTaxonomyMappingProperties()) //
          .execute().actionGet();

        client.admin().indices().preparePutMapping(indexName) //
          .setType(TaxonomyIndexer.TAXONOMY_VOCABULARY_TYPE).setSource(createVocabularyMappingProperties()) //
          .execute().actionGet();

        client.admin().indices().preparePutMapping(indexName) //
          .setType(TaxonomyIndexer.TAXONOMY_TERM_TYPE).setSource(createTermMappingProperties()) //
          .execute().actionGet();
      } catch(IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private XContentBuilder createTaxonomyMappingProperties() throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(TaxonomyIndexer.TAXONOMY_TYPE);
    mapping.startObject("properties");
    createMappingWithoutAnalyzer(mapping, "id");
    createMappingWithoutAnalyzer(mapping, "target");
    createMappingWithAndWithoutAnalyzer(mapping, "name");
    Stream.of(TaxonomyIndexer.LOCALIZED_ANALYZED_FIELDS)
      .forEach(field -> createLocalizedMappingWithAnalyzers(mapping, field));
    mapping.endObject(); // properties
    mapping.endObject().endObject();
    return mapping;
  }

  private XContentBuilder createVocabularyMappingProperties() throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject()
      .startObject(TaxonomyIndexer.TAXONOMY_VOCABULARY_TYPE);
    mapping.startObject("properties");
    createMappingWithoutAnalyzer(mapping, "id");
    createMappingWithoutAnalyzer(mapping, "target");
    createMappingWithAndWithoutAnalyzer(mapping, "name");
    createMappingWithAndWithoutAnalyzer(mapping, "taxonomyName");
    Stream.of(TaxonomyIndexer.LOCALIZED_ANALYZED_FIELDS)
      .forEach(field -> createLocalizedMappingWithAnalyzers(mapping, field));
    mapping.endObject(); // properties
    mapping.endObject().endObject();
    return mapping;
  }

  private XContentBuilder createTermMappingProperties() throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject()
      .startObject(TaxonomyIndexer.TAXONOMY_TERM_TYPE);
    mapping.startObject("properties");
    createMappingWithoutAnalyzer(mapping,"id");
    createMappingWithoutAnalyzer(mapping, "target");
    createMappingWithAndWithoutAnalyzer(mapping, "name");
    createMappingWithAndWithoutAnalyzer(mapping, "taxonomyName");
    createMappingWithAndWithoutAnalyzer(mapping, "vocabularyName");
    Stream.of(TaxonomyIndexer.LOCALIZED_ANALYZED_FIELDS)
      .forEach(field -> createLocalizedMappingWithAnalyzers(mapping, field));
    mapping.endObject(); // properties
    mapping.endObject().endObject();
    return mapping;
  }

}
