/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.obiba.mica.core.domain.TaxonomyTarget;
import org.obiba.mica.search.AbstractIndexConfiguration;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StudyIndexConfiguration extends AbstractIndexConfiguration
  implements ElasticSearchIndexer.IndexConfigurationListener {
  private static final Logger log = LoggerFactory.getLogger(StudyIndexConfiguration.class);

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
    mapping.startObject("properties");
    appendMembershipProperties(mapping);
    Taxonomy taxonomy = getTaxonomy();
    addLocalizedVocabularies(taxonomy, "name", "acronym");
    addStaticVocabularies(taxonomy, //
      "populations.dataCollectionEvents.start.yearMonth", //
      "populations.dataCollectionEvents.end.yearMonth");
    List<String> ignore = Lists.newArrayList(
      "memberships.investigator.person.fullName",
      "memberships.investigator.person.institution.name.und",
      "memberships.contact.person.fullName",
      "memberships.contact.person.institution.name.und"
    );
    addTaxonomyFields(mapping, taxonomy, ignore);
    mapping.endObject();

    return mapping;
  }

  @Override
  protected TaxonomyTarget getTarget() {
    return TaxonomyTarget.STUDY;
  }
}
