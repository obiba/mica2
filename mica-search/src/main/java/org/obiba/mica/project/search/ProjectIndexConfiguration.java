/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.search;

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
public class ProjectIndexConfiguration extends AbstractIndexConfiguration
  implements ElasticSearchIndexer.IndexConfigurationListener {
  private static final Logger log = LoggerFactory.getLogger(ProjectIndexConfiguration.class);

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(ProjectIndexer.DRAFT_PROJECT_INDEX.equals(indexName) ||
      ProjectIndexer.PUBLISHED_PROJECT_INDEX.equals(indexName)) {

      try {
        client.admin().indices().preparePutMapping(indexName).setType(ProjectIndexer.PROJECT_TYPE)
          .setSource(createMappingProperties()).execute().actionGet();
      } catch(IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private XContentBuilder createMappingProperties() throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(ProjectIndexer.PROJECT_TYPE);

    // properties
    mapping.startObject("properties");
    mapping.startObject("id").field("type", "string").field("index", "not_analyzed").endObject();
    appendMembershipProperties(mapping);
    Stream.of(ProjectIndexer.LOCALIZED_ANALYZED_FIELDS)
      .forEach(field -> createLocalizedMappingWithAnalyzers(mapping, field));
    mapping.endObject();

    mapping.endObject().endObject();
    return mapping;
  }

}
