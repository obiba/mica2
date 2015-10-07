/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.contact.search;

import java.io.IOException;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.obiba.mica.dataset.search.VariableIndexerImpl;
import org.obiba.mica.search.AbstractIndexConfiguration;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ContactIndexConfiguration extends AbstractIndexConfiguration implements ElasticSearchIndexer.IndexConfigurationListener {
  private static final Logger log = LoggerFactory.getLogger(ContactIndexConfiguration.class);

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(ContactIndexer.CONTACT_INDEX.equals(indexName)) {
      try {
        client.admin().indices().preparePutMapping(indexName).setType(ContactIndexer.CONTACT_TYPE)
          .setSource(createMappingProperties(ContactIndexer.CONTACT_TYPE)).execute().actionGet();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private XContentBuilder createMappingProperties(String type) throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(type);
    mapping.startObject("properties");
    mapping.startObject("id").field("type", "string").field("index", "not_analyzed").endObject();
    mapping.startObject("studyIds").field("type", "string").field("index", "not_analyzed").endObject();
    mapping.startObject("networkIds").field("type", "string").field("index", "not_analyzed").endObject();

    mapping.startObject("institution");
    mapping.startObject("properties");
    createLocalizedMappingWithAnalyzers(mapping, "name");
    mapping.endObject().endObject();

    createMappingWithAndWithoutAnalyzer(mapping, "firstName");
    createMappingWithAndWithoutAnalyzer(mapping, "lastName");
    createMappingWithAndWithoutAnalyzer(mapping, "email");
    mapping.endObject(); // properties
    mapping.endObject().endObject();

    return mapping;
  }
}
