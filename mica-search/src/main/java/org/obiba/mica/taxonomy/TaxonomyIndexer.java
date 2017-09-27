/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.obiba.mica.core.domain.TaxonomyTarget;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.service.TaxonomyService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;

@Component
public class TaxonomyIndexer {

  private static final Logger log = LoggerFactory.getLogger(TaxonomyIndexer.class);

  public static final String TAXONOMY_INDEX = "taxonomy";

  public static final String TAXONOMY_TYPE = "Taxonomy";

  public static final String TAXONOMY_VOCABULARY_TYPE = "Vocabulary";

  public static final String TAXONOMY_TERM_TYPE = "Term";

  public static final String[] LOCALIZED_ANALYZED_FIELDS = { "title", "description", "keywords" };

  @Inject
  private TaxonomyService taxonomyService;

  @Inject
  private Indexer indexer;

  @Async
  @Subscribe
  public void taxonomiesUpdated(TaxonomiesUpdatedEvent event) {
    // reindex all taxonomies if target is TAXONOMY or there is no target
    if ((event.getTaxonomyTarget() == null && event.getTaxonomyName() == null) || event.getTaxonomyTarget() == TaxonomyTarget.TAXONOMY) {
      log.info("All taxonomies were updated");
      if(indexer.hasIndex(TAXONOMY_INDEX)) indexer.dropIndex(TAXONOMY_INDEX);
      index(TaxonomyTarget.VARIABLE,
        ImmutableList.<Taxonomy>builder().addAll(taxonomyService.getOpalTaxonomies().stream() //
          .filter(t -> taxonomyService.metaTaxonomyContains(t.getName())).collect(Collectors.toList())) //
          .add(taxonomyService.getVariableTaxonomy()) //
          .build());
      index(TaxonomyTarget.STUDY, Lists.newArrayList(taxonomyService.getStudyTaxonomy()));
      index(TaxonomyTarget.DATASET, Lists.newArrayList(taxonomyService.getDatasetTaxonomy()));
      index(TaxonomyTarget.NETWORK, Lists.newArrayList(taxonomyService.getNetworkTaxonomy()));
    } else {
      Map.Entry<String, String> termQuery = ImmutablePair.of("taxonomyName", event.getTaxonomyName());
      indexer.delete(TAXONOMY_INDEX, new String[] {TAXONOMY_TYPE, TAXONOMY_VOCABULARY_TYPE, TAXONOMY_TERM_TYPE}, termQuery);

      switch (event.getTaxonomyTarget()) {
        case STUDY:
          log.info("Study taxonomies were updated");
          index(TaxonomyTarget.STUDY, Lists.newArrayList(taxonomyService.getStudyTaxonomy()));
          break;
        case NETWORK:
          log.info("Network taxonomies were updated");
          index(TaxonomyTarget.NETWORK, Lists.newArrayList(taxonomyService.getNetworkTaxonomy()));
          break;
        case DATASET:
          log.info("Dataset taxonomies were updated");
          index(TaxonomyTarget.DATASET, Lists.newArrayList(taxonomyService.getDatasetTaxonomy()));
          break;
        case VARIABLE:
          log.info("Variable taxonomies were updated");
          index(TaxonomyTarget.VARIABLE, Lists.newArrayList(taxonomyService.getVariableTaxonomy()));
          break;
      }
    }
  }

  private void index(TaxonomyTarget target, List<Taxonomy> taxonomies) {
    taxonomies.forEach(taxo -> {
      indexer.index(TAXONOMY_INDEX, new TaxonomyIndexable(target, taxo));
      if(taxo.hasVocabularies()) taxo.getVocabularies().forEach(voc -> {
        indexer.index(TAXONOMY_INDEX, new TaxonomyVocabularyIndexable(target, taxo, voc));
        if(voc.hasTerms()) voc.getTerms().forEach(
          term -> indexer.index(TAXONOMY_INDEX, new TaxonomyTermIndexable(target, taxo, voc, term)));
      });
    });
  }

}
