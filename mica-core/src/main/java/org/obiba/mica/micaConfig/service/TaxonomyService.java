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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUnpublishedEvent;
import org.obiba.mica.micaConfig.service.helper.DatasetIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.DceIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.NetworkIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.StudyIdAggregationMetaDataHelper;
import org.obiba.mica.network.event.NetworkPublishedEvent;
import org.obiba.mica.network.event.NetworkUnpublishedEvent;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.mica.study.event.StudyUnpublishedEvent;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.Subscribe;

@Service
public class TaxonomyService {

  @Inject
  private OpalService opalService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  StudyIdAggregationMetaDataHelper studyHelper;

  @Inject
  DatasetIdAggregationMetaDataHelper datasetHelper;

  @Inject
  NetworkIdAggregationMetaDataHelper networkHelper;

  @Inject
  DceIdAggregationMetaDataHelper dceHelper;

  private ReentrantLock txLock = new ReentrantLock();

  private Taxonomy variableTaxonomy;

  private Taxonomy datasetTaxonomy;

  private Taxonomy studyTaxonomy;

  private Taxonomy networkTaxonomy;

  @NotNull
  public Taxonomy getTaxonomyTaxonomy() {
    return micaConfigService.getTaxonomyTaxonomy();
  }

  @NotNull
  public Taxonomy getNetworkTaxonomy() {
    initialize();
    return networkTaxonomy;
  }

  @NotNull
  public Taxonomy getStudyTaxonomy() {
    initialize();
    return studyTaxonomy;
  }

  @NotNull
  public Taxonomy getDatasetTaxonomy() {
    initialize();
    return datasetTaxonomy;
  }

  @NotNull
  public Taxonomy getVariableTaxonomy() {
    initialize();
    return variableTaxonomy;
  }

  @NotNull
  public List<Taxonomy> getVariableTaxonomies() {
    return Stream.concat(getOpalTaxonomies().stream(), Stream.of(getVariableTaxonomy())).collect(Collectors.toList());
  }

  @NotNull
  public synchronized List<Taxonomy> getOpalTaxonomies() {
    List<Taxonomy> taxonomies = null;

    try {
      taxonomies = opalService.getTaxonomies();
    } catch(Exception e) {
      // ignore
    }

    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }

  //
  // Private methods
  //

  private void refresh() {
    txLock.lock();
    networkTaxonomy = null;
    studyTaxonomy = null;
    datasetTaxonomy = null;
    variableTaxonomy = null;
    txLock.unlock();
  }

  private void initialize() {
    txLock.lock();
    try {
      initializeNetworkTaxonomy();
      initializeStudyTaxonomy();
      initializeDatasetTaxonomy();
      initializeVariableTaxonomy();
    } finally {
      txLock.unlock();
    }
  }

  private void initializeNetworkTaxonomy() {
    if(networkTaxonomy != null) return;
    networkTaxonomy = copy(micaConfigService.getNetworkTaxonomy());
    networkHelper.applyIdTerms(networkTaxonomy, "id");
    studyHelper.applyIdTerms(networkTaxonomy, "studyIds");
  }

  private void initializeStudyTaxonomy() {
    if(studyTaxonomy != null) return;
    studyTaxonomy = copy(micaConfigService.getStudyTaxonomy());
    studyHelper.applyIdTerms(studyTaxonomy, "id");
    dceHelper.applyIdTerms(studyTaxonomy, "dceIds");
  }

  private void initializeDatasetTaxonomy() {
    if(datasetTaxonomy != null) return;
    datasetTaxonomy = copy(micaConfigService.getDatasetTaxonomy());
    datasetHelper.applyIdTerms(datasetTaxonomy, "id");
    networkHelper.applyIdTerms(datasetTaxonomy, "networkId");
  }

  private void initializeVariableTaxonomy() {
    if(variableTaxonomy != null) return;
    variableTaxonomy = copy(micaConfigService.getVariableTaxonomy());
    studyHelper.applyIdTerms(variableTaxonomy, "studyIds");
    datasetHelper.applyIdTerms(variableTaxonomy, "datasetId");
    networkHelper.applyIdTerms(variableTaxonomy, "networkId");
    dceHelper.applyIdTerms(variableTaxonomy, "dceIds");
  }

  private Taxonomy copy(Taxonomy source) {
    Taxonomy target = new Taxonomy();
    BeanUtils.copyProperties(source, target, "vocabularies");
    if(source.hasVocabularies()) {
      source.getVocabularies().forEach(sourceVoc -> {
        Vocabulary targetVoc = new Vocabulary();
        BeanUtils.copyProperties(sourceVoc, targetVoc, "terms");
        if(sourceVoc.hasTerms()) {
          sourceVoc.getTerms().forEach(sourceTerm -> {
            Term targetTerm = new Term();
            BeanUtils.copyProperties(sourceTerm, targetTerm);
            targetVoc.addTerm(targetTerm);
          });
        }
        target.addVocabulary(targetVoc);
      });
    }

    return target;
  }

  //
  // Event handling
  //

  @Async
  @Subscribe
  public void networkPublished(NetworkPublishedEvent event) {
    refresh();
  }

  @Async
  @Subscribe
  public void networkUnpublished(NetworkUnpublishedEvent event) {
    refresh();
  }

  @Async
  @Subscribe
  public void studyPublished(StudyPublishedEvent event) {
    refresh();
  }

  @Async
  @Subscribe
  public void studyUnpublished(StudyUnpublishedEvent event) {
    refresh();
  }

  @Async
  @Subscribe
  public void datasetPublished(DatasetPublishedEvent event) {
    refresh();
  }

  @Async
  @Subscribe
  public void datasetUnpublished(DatasetUnpublishedEvent event) {
    refresh();
  }

}
