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

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.obiba.mica.core.domain.AttributeKey;
import org.obiba.mica.micaConfig.OpalService;
import org.obiba.mica.search.AbstractIndexConfiguration;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.stereotype.Component;

@Component
public class VariableIndexConfiguration extends AbstractIndexConfiguration implements ElasticSearchIndexer.IndexConfigurationListener {

  @Inject
  private OpalService opalService;

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(VariableIndexer.DRAFT_VARIABLE_INDEX.equals(indexName) ||
        VariableIndexer.PUBLISHED_VARIABLE_INDEX.equals(indexName)) {
      setMappingProperties(client, indexName);
    }
  }

  private void setMappingProperties(Client client, String indexName) {
    try {
      client.admin().indices().preparePutMapping(indexName).setType(VariableIndexer.VARIABLE_TYPE)
          .setSource(createMappingProperties(VariableIndexer.VARIABLE_TYPE)).execute().actionGet();
      client.admin().indices().preparePutMapping(indexName).setType(VariableIndexer.HARMONIZED_VARIABLE_TYPE)
          .setSource(createMappingProperties(VariableIndexer.HARMONIZED_VARIABLE_TYPE)).execute().actionGet();
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  private XContentBuilder createMappingProperties(String type) throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(type);

    // properties
    mapping.startObject("properties");
      mapping.startObject("id").field("type", "string").field("index", "not_analyzed").endObject();
    mapping.startObject("studyIds").field("type", "string").field("index", "not_analyzed").endObject();
    mapping.startObject("dceIds").field("type", "string").field("index", "not_analyzed").endObject();
    mapping.startObject("datasetId").field("type", "string").field("index", "not_analyzed").endObject();
    createMappingWithAndWithoutAnalyzer(mapping, "name");

    // attributes from taxonomies
    try {
      List<Taxonomy> taxonomies = opalService.getTaxonomies();
      if(taxonomies != null && !taxonomies.isEmpty()) {
        mapping.startObject("attributes");
        mapping.startObject("properties");
        createMappingTaxonomies(mapping, taxonomies);
        Stream.of(VariableIndexer.LOCALIZED_ANALYZED_FIELDS).forEach(field -> createLocalizedMappingWithAnalyzers(mapping, field));
        mapping.endObject(); // properties
        mapping.endObject(); // attributes
      }
    } catch(Exception e) {
      // ignore
    }

    mapping.endObject(); // properties

    // parent
    if(VariableIndexer.HARMONIZED_VARIABLE_TYPE.equals(type)) {
      mapping.startObject("_parent").field("type", VariableIndexer.VARIABLE_TYPE).endObject();
    }

    mapping.endObject().endObject();
    return mapping;
  }

  private void createMappingTaxonomies(XContentBuilder mapping, List<Taxonomy> taxonomies) throws IOException {
    for(Taxonomy taxonomy : taxonomies) {
      if(taxonomy.hasVocabularies()) {
        createMappingTaxonomy(mapping, taxonomy);
      }
    }
  }

  private void createMappingTaxonomy(XContentBuilder mapping, Taxonomy taxonomy) throws IOException {
    for(Vocabulary vocabulary : taxonomy.getVocabularies()) {
      String namespace = taxonomy.getName().equals("Default") ? null : taxonomy.getName();
      String field = AttributeKey.getMapKey(vocabulary.getName(), namespace);
      if(vocabulary.hasTerms()) {
        // not analyzed: we want exact match
        mapping.startObject(field);
        mapping.startObject("properties");
        mapping.startObject("und").field("type", "string").field("index", "not_analyzed").endObject();
        mapping.endObject(); // properties
        mapping.endObject(); // field
      }
    }
  }

}
