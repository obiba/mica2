/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.person.search;

import java.io.IOException;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.obiba.mica.search.AbstractIndexConfiguration;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.obiba.mica.spi.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PersonIndexConfiguration extends AbstractIndexConfiguration {
  private static final Logger log = LoggerFactory.getLogger(PersonIndexConfiguration.class);

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(Indexer.PERSON_INDEX.equals(indexName)) {
      try {
        client.admin().indices().preparePutMapping(indexName).setType(Indexer.PERSON_TYPE)
          .setSource(createMappingProperties(Indexer.PERSON_TYPE)).execute().actionGet();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private XContentBuilder createMappingProperties(String type) throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(type);
    mapping.startObject("properties");
    mapping.startObject("id").field("type", "string").field("index", "not_analyzed").endObject();
    mapping.startObject("studyMemberships").startObject("properties");
    mapping.startObject("parentId").field("type", "string").field("index", "not_analyzed").endObject();
    mapping.startObject("role").field("type", "string").field("index", "not_analyzed").endObject();
    mapping.endObject().endObject();
    mapping.startObject("networkMemberships").startObject("properties");
    mapping.startObject("parentId").field("type", "string").field("index", "not_analyzed").endObject();
    mapping.startObject("role").field("type", "string").field("index", "not_analyzed").endObject();
    mapping.endObject().endObject();
    mapping.startObject("institution");
    mapping.startObject("properties");
    createLocalizedMappingWithAnalyzers(mapping, "name");
    mapping.endObject().endObject();

    createMappingWithAndWithoutAnalyzer(mapping, "firstName");
    createMappingWithAndWithoutAnalyzer(mapping, "lastName");
    createMappingWithAndWithoutAnalyzer(mapping, "fullName");
    createMappingWithAndWithoutAnalyzer(mapping, "email");
    mapping.endObject(); // properties
    mapping.endObject().endObject();

    return mapping;
  }
}
