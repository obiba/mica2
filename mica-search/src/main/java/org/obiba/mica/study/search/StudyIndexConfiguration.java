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
import java.util.stream.Stream;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.obiba.mica.search.AbstractIndexConfiguration;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.springframework.stereotype.Component;

@Component
public class StudyIndexConfiguration extends AbstractIndexConfiguration
  implements ElasticSearchIndexer.IndexConfigurationListener {

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(StudyIndexer.DRAFT_STUDY_INDEX.equals(indexName) || StudyIndexer.PUBLISHED_STUDY_INDEX.equals(indexName)) {

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
    appendStudyProperties(mapping);

    // don't analyze YearMonth fields, preserve values as yyyy-mm
    mapping.startObject("populations").startObject("properties") //

      .startObject("recruitment").startObject("properties") //
      .startObject("dataSources").field("type", "string").field("index", "not_analyzed").endObject() //
      .startObject("generalPopulationSources").field("type", "string").field("index", "not_analyzed").endObject() //
      .startObject("specificPopulationSources").field("type", "string").field("index", "not_analyzed").endObject() //
      .endObject().endObject() // recruitment

      .startObject("selectionCriteria").startObject("properties") //
      .startObject("countriesIso").field("type", "string").field("index", "not_analyzed").endObject() //
      .startObject("criteria").field("type", "string").field("index", "not_analyzed").endObject() //
      .startObject("gender").field("type", "string").field("index", "not_analyzed").endObject() //
      .endObject().endObject() // selectionCriteria

      .startObject("dataCollectionEvents").startObject("properties") //
      .startObject("dataSources").field("type", "string").field("index", "not_analyzed").endObject() //
      .startObject("bioSamples").field("type", "string").field("index", "not_analyzed").endObject() //
      .startObject("administrativeDatabases").field("type", "string").field("index", "not_analyzed").endObject() //
      .startObject("start").startObject("properties") //
      .startObject("yearMonth").field("type", "string").field("index", "not_analyzed").endObject() //
      .endObject().endObject() // start
      .startObject("end").startObject("properties") //
      .startObject("yearMonth").field("type", "string").field("index", "not_analyzed").endObject() //
      .endObject().endObject() // end
      .endObject().endObject() // dataCollectionEvents

      .endObject().endObject(); // populations

    // memberships
    appendMembershipProperties(mapping);

    Stream.of(StudyIndexer.LOCALIZED_ANALYZED_FIELDS)
      .forEach(field -> createLocalizedMappingWithAnalyzers(mapping, field));
    mapping.endObject();
    mapping.endObject();

    return mapping;
  }

  private void appendStudyProperties(XContentBuilder mapping) throws IOException {
    createMappingWithoutAnalyzer(mapping, "id");
    mapping.startObject("methods").startObject("properties") //
      .startObject("designs").field("type", "string").field("index", "not_analyzed").endObject() //
      .startObject("recruitments").field("type", "string").field("index", "not_analyzed").endObject() //
      .endObject().endObject(); // methods
    createMappingWithoutAnalyzer(mapping, "access");
  }

}
