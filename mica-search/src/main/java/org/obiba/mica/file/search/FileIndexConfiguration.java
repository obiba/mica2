/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.search;

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
public class FileIndexConfiguration extends AbstractIndexConfiguration implements ElasticSearchIndexer.IndexConfigurationListener {
  private static final Logger log = LoggerFactory.getLogger(FileIndexConfiguration.class);

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(FileIndexer.ATTACHMENT_DRAFT_INDEX.equals(indexName) ||
      FileIndexer.ATTACHMENT_PUBLISHED_INDEX.equals(indexName)) {
      try {
        String attachmentField = FileIndexer.ATTACHMENT_DRAFT_INDEX.equals(indexName)
          ? "attachment"
          : "publishedAttachment";
        client.admin().indices().preparePutMapping(indexName).setType(FileIndexer.ATTACHMENT_TYPE)
          .setSource(createMappingProperties(FileIndexer.ATTACHMENT_TYPE, attachmentField)).execute().actionGet();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private XContentBuilder createMappingProperties(String type, String attachmentField) throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(type);
    mapping.startObject("properties");
    mapping.startObject("id").field("type", "string").field("index", "not_analyzed").endObject();
    createMappingWithAndWithoutAnalyzer(mapping, "name");
    mapping.startObject("path").field("type", "string").field("index", "not_analyzed").endObject();
    mapping.startObject(attachmentField).startObject("properties");
    createMappingWithAndWithoutAnalyzer(mapping, "type");
    createMappingWithAndWithoutAnalyzer(mapping, "name");
    createLocalizedMappingWithAnalyzers(mapping, "description");
    mapping.endObject().endObject(); // attachment
    mapping.endObject(); // properties
    mapping.endObject().endObject();

    return mapping;
  }
}
