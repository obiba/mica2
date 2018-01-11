/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import java.util.Map;

import javax.inject.Inject;

import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.config.taxonomies.DatasetTaxonomy;
import org.obiba.mica.config.taxonomies.NetworkTaxonomy;
import org.obiba.mica.config.taxonomies.StudyTaxonomy;
import org.obiba.mica.config.taxonomies.TaxonomyTaxonomy;
import org.obiba.mica.config.taxonomies.VariableTaxonomy;
import org.obiba.mica.core.domain.TaxonomyEntityWrapper;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.repository.TaxonomyConfigRepository;
import org.obiba.mica.spi.search.support.AggregationHelper;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;

@Service
@EnableConfigurationProperties({ NetworkTaxonomy.class, StudyTaxonomy.class, DatasetTaxonomy.class, VariableTaxonomy.class, TaxonomyTaxonomy.class })
public class TaxonomyConfigService {

  @Inject
  private TaxonomyConfigRepository taxonomyConfigRepository;

  @Inject
  private NetworkTaxonomy defaultNetworkTaxonomy;

  @Inject
  private StudyTaxonomy defaultStudyTaxonomy;

  @Inject
  private DatasetTaxonomy defaultDatasetTaxonomy;

  @Inject
  private VariableTaxonomy defaultVariableTaxonomy;

  @Inject
  private TaxonomyTaxonomy defaultTaxonomyTaxonomy;

  @Inject
  private EventBus eventBus;

  public Taxonomy findByTarget(TaxonomyTarget target) {
    return findByTargetInternal(target);
  }

  public void update(TaxonomyTarget target, Taxonomy taxonomy) {
    updateInternal(target, taxonomy);
    eventBus.post(new TaxonomiesUpdatedEvent(taxonomy.getName(), target));
  }

  public void mergeWithDefault(TaxonomyTarget target) {
    Taxonomy targetTaxonomy = findByTarget(target);
    mergeVocabulariesTerms(targetTaxonomy, getDefault(target));

    updateInternal(target, targetTaxonomy);
  }

  private Taxonomy findByTargetInternal(TaxonomyTarget target) {
    // taxonomy of taxonomies is not editable so fall back to the one that comes from the classpath
    if (TaxonomyTarget.TAXONOMY.equals(target))
      return defaultTaxonomyTaxonomy;
    String id = target.asId();
    TaxonomyEntityWrapper taxonomyEntityWrapper = taxonomyConfigRepository.findOne(id);

    if (taxonomyEntityWrapper == null) {
      createDefault(target);
      taxonomyEntityWrapper = taxonomyConfigRepository.findOne(id);
    }

    return taxonomyEntityWrapper.getTaxonomy();
  }

  void mergeVocabulariesTerms(Taxonomy taxonomy, Taxonomy defaultTaxonomy) {
    defaultTaxonomy.getVocabularies().forEach(v -> {
      if (!taxonomy.hasVocabulary(v.getName())) {
        taxonomy.addVocabulary(v);
      } else {
        Vocabulary defaultTaxonomyVocabulary = defaultTaxonomy.getVocabulary(v.getName());
        Vocabulary taxonomyVocabulary = taxonomy.getVocabulary(v.getName());

        if (defaultTaxonomyVocabulary.hasTerms()) {
          defaultTaxonomyVocabulary.getTerms().forEach(t -> {
            if (!taxonomyVocabulary.hasTerm(t.getName())) taxonomyVocabulary.addTerm(t);
          });
        }
      }
    });
  }

  private void updateInternal(TaxonomyTarget target, Taxonomy taxonomy) {
    validateTaxonomy(taxonomy);
    TaxonomyEntityWrapper taxonomyEntityWrapper = new TaxonomyEntityWrapper();
    taxonomyEntityWrapper.setTarget(target.asId());
    taxonomyEntityWrapper.setTaxonomy(taxonomy);
    taxonomyConfigRepository.save(taxonomyEntityWrapper);
  }

  void validateTaxonomy(Taxonomy taxonomy) {
    Map<String, Boolean> aliases = Maps.newHashMap();
    taxonomy.getVocabularies().forEach(v -> {
      String field = v.getAttributeValue("field");

      if (!Strings.isNullOrEmpty(field)) {
        String type = v.getAttributeValue("type");
        if (Strings.isNullOrEmpty(type)) type = "string";

        String alias = v.getAttributeValue("alias");
        if(Strings.isNullOrEmpty(alias)) alias = AggregationHelper.formatName(field);

        String range = v.getAttributeValue("range");
        boolean isRange = Strings.isNullOrEmpty(range) ? false : "true".equals(range);

        if(aliases.containsKey(alias)) throw new VocabularyDuplicateAliasException(v);
        aliases.put(alias, true);

        if("integer".equals(type) || "decimal".equals(type)) {
          if (v.hasTerms() != isRange) {
            if (isRange) throw new VocabularyMissingRangeTermsException(v);
            if (!isRange) throw new VocabularyMissingRangeAttributeException(v);
          }
        }
      }
    });
  }

  private void createDefault(TaxonomyTarget target) {
    updateInternal(target, getDefault(target));
  }

  private Taxonomy getDefault(TaxonomyTarget target) {
    switch(target) {
      case DATASET:
        return defaultDatasetTaxonomy;
      case STUDY:
        return defaultStudyTaxonomy;
      case NETWORK:
        return defaultNetworkTaxonomy;
      case VARIABLE:
        return defaultVariableTaxonomy;
      case TAXONOMY:
        return defaultTaxonomyTaxonomy;
    }

    throw new NoSuchEntityException("Invalid taxonomy target: " + target);
  }
}
