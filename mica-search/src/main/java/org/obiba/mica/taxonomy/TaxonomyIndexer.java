/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.obiba.mica.micaConfig.event.DeleteTaxonomiesEvent;
import org.obiba.mica.micaConfig.event.OpalTaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.service.TaxonomyService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TaxonomyIndexer {

  private static final Logger log = LoggerFactory.getLogger(TaxonomyIndexer.class);

  @Inject
  private TaxonomyService taxonomyService;

  @Inject
  private Indexer indexer;

  @Async
  @Subscribe
  public void opalTaxonomiesUpdatedEvent(OpalTaxonomiesUpdatedEvent event) {
    log.info("Reindex all opal taxonomies");
    index(
      TaxonomyTarget.VARIABLE,
      event.extractOpalTaxonomies()
        .stream()
        .filter(t -> taxonomyService.metaTaxonomyContains(t.getName()))
        .collect(Collectors.toList()));
  }

  @Async
  @Subscribe
  public void deletetaxonomies(DeleteTaxonomiesEvent event) {
    indexer.dropIndex("_all");
  }

  @Async
  @Subscribe
  public void taxonomiesUpdated(TaxonomiesUpdatedEvent event) {
    // reindex all taxonomies if target is TAXONOMY or there is no target
    if ((event.getTaxonomyTarget() == null && event.getTaxonomyName() == null) || event.getTaxonomyTarget() == TaxonomyTarget.TAXONOMY) {

      log.info("All taxonomies were updated");
      if(indexer.hasIndex(Indexer.TAXONOMY_INDEX)) indexer.dropIndex(Indexer.TAXONOMY_INDEX);
      if(indexer.hasIndex(Indexer.VOCABULARY_INDEX)) indexer.dropIndex(Indexer.VOCABULARY_INDEX);
      if(indexer.hasIndex(Indexer.TERM_INDEX)) indexer.dropIndex(Indexer.TERM_INDEX);

      index(TaxonomyTarget.VARIABLE,
        ImmutableList.<Taxonomy>builder().addAll(taxonomyService.getOpalTaxonomies().stream() //
          .filter(t -> taxonomyService.metaTaxonomyContains(t.getName())).collect(Collectors.toList())) //
          .add(taxonomyService.getVariableTaxonomy()) //
          .build());
      index(TaxonomyTarget.STUDY, Lists.newArrayList(taxonomyService.getStudyTaxonomy()));
      index(TaxonomyTarget.DATASET, Lists.newArrayList(taxonomyService.getDatasetTaxonomy()));
      index(TaxonomyTarget.NETWORK, Lists.newArrayList(taxonomyService.getNetworkTaxonomy()));
    } else {
      indexer.delete(Indexer.TAXONOMY_INDEX, new String[] {}, ImmutablePair.of("name", event.getTaxonomyName()));
      Map.Entry<String, String> termQuery = ImmutablePair.of("taxonomyName", event.getTaxonomyName());
      indexer.delete(Indexer.VOCABULARY_INDEX, new String[] {}, termQuery);
      indexer.delete(Indexer.TERM_INDEX, new String[] {}, termQuery);

      Taxonomy taxonomy = null;
      TaxonomyTarget taxonomyTarget = event.getTaxonomyTarget();

      switch (taxonomyTarget) {
        case STUDY:
          log.info("Study taxonomies were updated");
          taxonomy = taxonomyService.getStudyTaxonomy();
          break;
        case NETWORK:
          log.info("Network taxonomies were updated");
          taxonomy = taxonomyService.getNetworkTaxonomy();
          break;
        case DATASET:
          log.info("Dataset taxonomies were updated");
          taxonomy = taxonomyService.getDatasetTaxonomy();
          break;
        case VARIABLE:
          log.info("Variable taxonomies were updated");
          taxonomy = taxonomyService.getVariableTaxonomy();
          break;
      }

      if (taxonomy != null) {
        index(taxonomyTarget, Lists.newArrayList(taxonomy));
      }
    }
  }

  private void index(TaxonomyTarget target, List<Taxonomy> taxonomies) {
    taxonomies.forEach(taxo -> {
      indexer.index(Indexer.TAXONOMY_INDEX, new TaxonomyIndexable(target, taxo));
      if(taxo.hasVocabularies()) {
        List<Vocabulary> vocabularies = taxo.getVocabularies();

        indexer.indexAllIndexables(Indexer.VOCABULARY_INDEX, vocabularies.stream().map(voc -> new TaxonomyVocabularyIndexable(target, taxo, voc)).collect(Collectors.toList()));
        vocabularies.forEach(voc -> {
          if (voc.hasTerms()) {
            indexer.indexAllIndexables(Indexer.TERM_INDEX, voc.getTerms().stream().map(term -> new TaxonomyTermIndexable(target, taxo, voc, term)).collect(Collectors.toList()));
          }
        });
      }
    });
  }
}
