/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy;

import javax.inject.Inject;

import org.obiba.mica.core.domain.Indexable;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class TaxonomyIndexer {

  private static final Logger log = LoggerFactory.getLogger(TaxonomyIndexer.class);

  public static final String TAXONOMY_INDEX = "taxonomy";

  public static final String TAXONOMY_TERM_TYPE = "Term";

  @Inject
  private OpalService opalService;

  @Inject
  private ElasticSearchIndexer elasticSearchIndexer;

  @Async
  @Subscribe
  public void taxonomiesUpdated(TaxonomiesUpdatedEvent event) {
    log.info("Taxonomies {} were updated");
    if(elasticSearchIndexer.hasIndex(TAXONOMY_INDEX)) elasticSearchIndexer.dropIndex(TAXONOMY_INDEX);
    opalService.getTaxonomies().forEach(taxo -> taxo.getVocabularies().forEach(voc -> voc.getTerms()
      .forEach(term -> elasticSearchIndexer.index(TAXONOMY_INDEX, new TaxonomyTerm(taxo, voc, term)))));
  }

  public static class TaxonomyTerm implements Indexable {

    private final String taxonomyName;

    private final String vocabularyName;

    private final String name;

    public TaxonomyTerm(Taxonomy taxonomy, Vocabulary vocabulary, Term term) {
      taxonomyName = taxonomy.getName();
      vocabularyName = vocabulary.getName();
      name = term.getName();
    }

    @Override
    public String getId() {
      return getParentId() + ":" + name;
    }

    @Override
    public String getClassName() {
      return null;
    }

    @Override
    public String getMappingName() {
      return TAXONOMY_TERM_TYPE;
    }

    @Override
    public String getParentId() {
      return taxonomyName + ":" + vocabularyName;
    }

    public String getTaxonomyName() {
      return taxonomyName;
    }

    public String getVocabularyName() {
      return vocabularyName;
    }

    public String getName() {
      return name;
    }
  }

}
