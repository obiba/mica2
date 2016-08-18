/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import java.util.Optional;

import javax.inject.Inject;

import com.google.common.eventbus.EventBus;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.config.taxonomies.DatasetTaxonomy;
import org.obiba.mica.config.taxonomies.NetworkTaxonomy;
import org.obiba.mica.config.taxonomies.StudyTaxonomy;
import org.obiba.mica.config.taxonomies.TaxonomyTaxonomy;
import org.obiba.mica.config.taxonomies.VariableTaxonomy;
import org.obiba.mica.core.domain.TaxonomyEntityWrapper;
import org.obiba.mica.core.domain.TaxonomyTarget;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.obiba.mica.micaConfig.repository.TaxonomyConfigRepository;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

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
    getEvent(target).ifPresent(eventBus::post);
  }

  private Taxonomy findByTargetInternal(TaxonomyTarget target) {
    String id = target.asId();
    TaxonomyEntityWrapper taxonomyEntityWrapper = taxonomyConfigRepository.findOne(id);

    if (taxonomyEntityWrapper == null) {
      createDefault(target);
      taxonomyEntityWrapper = taxonomyConfigRepository.findOne(id);
    }

    return taxonomyEntityWrapper.getTaxonomy();
  }

  private void updateInternal(TaxonomyTarget target, Taxonomy taxonomy) {
    TaxonomyEntityWrapper taxonomyEntityWrapper = new TaxonomyEntityWrapper();
    taxonomyEntityWrapper.setTarget(target.asId());
    taxonomyEntityWrapper.setTaxonomy(taxonomy);
    taxonomyConfigRepository.save(taxonomyEntityWrapper);
  }

  private Optional<Object> getEvent(TaxonomyTarget target) {
    Object event = null;

    switch (target) {
      case STUDY:
        event = new IndexStudiesEvent();
        break;
      case NETWORK:
        event = new IndexNetworksEvent();
        break;
      case DATASET:
        event = new IndexDatasetsEvent();
        break;
    }

    return Optional.ofNullable(event);
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
