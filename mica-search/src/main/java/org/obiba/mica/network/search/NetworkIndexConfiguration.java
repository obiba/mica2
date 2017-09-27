/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.search;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.search.AbstractIndexConfiguration;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NetworkIndexConfiguration extends AbstractIndexConfiguration {
  private static final Logger log = LoggerFactory.getLogger(NetworkIndexConfiguration.class);

  @Override
  public void onIndexCreated(Client client, String indexName) {
    if(Indexer.DRAFT_NETWORK_INDEX.equals(indexName) ||
      Indexer.PUBLISHED_NETWORK_INDEX.equals(indexName)) {

      try {
        client.admin().indices().preparePutMapping(indexName).setType(Indexer.NETWORK_TYPE)
          .setSource(createMappingProperties()).execute().actionGet();
      } catch(IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private XContentBuilder createMappingProperties() throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(Indexer.NETWORK_TYPE);

    mapping.startObject("properties");
    appendMembershipProperties(mapping);
    Taxonomy taxonomy = getTaxonomy();
    taxonomy.addVocabulary(newVocabularyBuilder().name("raw_id").field("id").staticField().build());
    addLocalizedVocabularies(taxonomy, "acronym", "name", "description");
    List<String> ignore = Lists.newArrayList(
      "memberships.investigator.person.fullName",
      "memberships.investigator.person.institution.name.und",
      "memberships.contact.person.fullName",
      "memberships.contact.person.institution.name.und"
    );

    addTaxonomyFields(mapping, taxonomy, ignore);

    mapping.endObject().endObject();
    return mapping;
  }

  @Override
  protected TaxonomyTarget getTarget() {
    return TaxonomyTarget.NETWORK;
  }
}
