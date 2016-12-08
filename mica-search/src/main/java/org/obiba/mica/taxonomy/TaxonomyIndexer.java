/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.obiba.mica.core.domain.TaxonomyTarget;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.service.TaxonomyService;
import org.obiba.mica.search.ElasticSearchIndexer;
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
  private ElasticSearchIndexer elasticSearchIndexer;

  @Async
  @Subscribe
  public void taxonomiesUpdated(TaxonomiesUpdatedEvent event) {
    // reindex all taxonomies if target is TAXONOMY or there is no target
    if ((event.getTaxonomyTarget() == null && event.getTaxonomyName() == null) || event.getTaxonomyTarget() == TaxonomyTarget.TAXONOMY) {
      log.info("All taxonomies were updated");
      if(elasticSearchIndexer.hasIndex(TAXONOMY_INDEX)) elasticSearchIndexer.dropIndex(TAXONOMY_INDEX);
      index(TaxonomyTarget.VARIABLE,
        ImmutableList.<Taxonomy>builder().addAll(taxonomyService.getOpalTaxonomies().stream() //
          .filter(t -> taxonomyService.metaTaxonomyContains(t.getName())).collect(Collectors.toList())) //
          .add(taxonomyService.getVariableTaxonomy()) //
          .build());
      index(TaxonomyTarget.STUDY, Lists.newArrayList(taxonomyService.getStudyTaxonomy()));
      index(TaxonomyTarget.DATASET, Lists.newArrayList(taxonomyService.getDatasetTaxonomy()));
      index(TaxonomyTarget.NETWORK, Lists.newArrayList(taxonomyService.getNetworkTaxonomy()));
    } else {
      QueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.wildcardQuery("id", event.getTaxonomyName() + '*'));

      elasticSearchIndexer.delete(TAXONOMY_INDEX, TAXONOMY_TYPE, query);
      elasticSearchIndexer.delete(TAXONOMY_INDEX, TAXONOMY_VOCABULARY_TYPE, query);
      elasticSearchIndexer.delete(TAXONOMY_INDEX, TAXONOMY_TERM_TYPE, query);

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
      elasticSearchIndexer.index(TAXONOMY_INDEX, new TaxonomyIndexable(target, taxo));
      if(taxo.hasVocabularies()) taxo.getVocabularies().forEach(voc -> {
        elasticSearchIndexer.index(TAXONOMY_INDEX, new TaxonomyVocabularyIndexable(target, taxo, voc));
        if(voc.hasTerms()) voc.getTerms().forEach(
          term -> elasticSearchIndexer.index(TAXONOMY_INDEX, new TaxonomyTermIndexable(target, taxo, voc, term)));
      });
    });
  }

}
