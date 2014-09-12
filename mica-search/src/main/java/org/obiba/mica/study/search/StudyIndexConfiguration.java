/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search;

import java.io.IOException;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.springframework.stereotype.Component;

@Component
public class StudyIndexConfiguration implements ElasticSearchIndexer.IndexConfigurationListener {

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(StudyIndexer.DRAFT_STUDY_INDEX.equals(indexName) ||
        StudyIndexer.PUBLISHED_STUDY_INDEX.equals(indexName)) {

      try {
        client.admin().indices().preparePutMapping(indexName).setType(StudyIndexer.STUDY_TYPE)
            .setSource(createMappingProperties()).execute().actionGet();
      } catch(IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private XContentBuilder createMappingProperties() throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(StudyIndexer.STUDY_TYPE);

    // properties
    mapping.startObject("properties");
    mapping.startObject("id").field("type", "string").field("index","not_analyzed").endObject();
    mapping.endObject();

    mapping.endObject().endObject();
    return mapping;
  }

}
