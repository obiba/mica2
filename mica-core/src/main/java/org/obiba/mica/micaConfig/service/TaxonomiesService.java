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

import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import org.obiba.mica.core.event.DocumentSetUpdatedEvent;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUnpublishedEvent;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.helper.*;
import org.obiba.mica.network.event.NetworkPublishedEvent;
import org.obiba.mica.network.event.NetworkUnpublishedEvent;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.mica.study.event.StudyUnpublishedEvent;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TaxonomiesService {

  private final VariableTaxonomiesService variableTaxonomiesService;

  private final MicaConfigService micaConfigService;

  private final StudyIdAggregationMetaDataHelper studyHelper;

  private final DatasetIdAggregationMetaDataHelper datasetHelper;

  private final NetworkIdAggregationMetaDataHelper networkHelper;

  private final PopulationIdAggregationMetaDataHelper populationHelper;

  private final DceIdAggregationMetaDataHelper dceHelper;

  private final NetworksSetsAggregationMetaDataHelper networksSetsAggregationMetaDataHelper;

  private final StudiesSetsAggregationMetaDataHelper studiesSetsHelper;

  private final VariablesSetsAggregationMetaDataHelper variablesSetsHelper;

  private final TaxonomyConfigService taxonomyConfigService;

  private Taxonomy taxonomyTaxonomy;

  private Taxonomy variableTaxonomy;

  private Taxonomy datasetTaxonomy;

  private Taxonomy studyTaxonomy;

  private Taxonomy networkTaxonomy;

  @Inject
  public TaxonomiesService(
    VariableTaxonomiesService variableTaxonomiesService,
    MicaConfigService micaConfigService,
    StudyIdAggregationMetaDataHelper studyHelper,
    DatasetIdAggregationMetaDataHelper datasetHelper,
    NetworkIdAggregationMetaDataHelper networkHelper,
    PopulationIdAggregationMetaDataHelper populationHelper,
    DceIdAggregationMetaDataHelper dceHelper,
    NetworksSetsAggregationMetaDataHelper networksSetsAggregationMetaDataHelper,
    StudiesSetsAggregationMetaDataHelper studiesSetsHelper,
    VariablesSetsAggregationMetaDataHelper variablesSetsHelper,
    TaxonomyConfigService taxonomyConfigService) {
    this.variableTaxonomiesService = variableTaxonomiesService;
    this.micaConfigService = micaConfigService;
    this.studyHelper = studyHelper;
    this.datasetHelper = datasetHelper;
    this.networkHelper = networkHelper;
    this.populationHelper = populationHelper;
    this.dceHelper = dceHelper;
    this.networksSetsAggregationMetaDataHelper = networksSetsAggregationMetaDataHelper;
    this.studiesSetsHelper = studiesSetsHelper;
    this.variablesSetsHelper = variablesSetsHelper;
    this.taxonomyConfigService = taxonomyConfigService;
  }

  @NotNull
  public Taxonomy getTaxonomyTaxonomy() {
    initialize();
    return taxonomyTaxonomy;
  }

  public boolean metaTaxonomyContains(String taxonomy) {
    for (Vocabulary targetVocabulary : getTaxonomyTaxonomy().getVocabularies()) {
      Optional<Term> termOpt = getTerm(targetVocabulary, taxonomy);
      if (termOpt.isPresent()) {
        Term term = termOpt.get();
        String hidden = term.getAttributeValue("hidden");
        // visible by default
        if (Strings.isNullOrEmpty(hidden)) return true;
        // check visible attribute value
        try {
          return !Boolean.parseBoolean(hidden.toLowerCase());
        } catch (Exception e) {
          return false;
        }
      }
    }
    return false;
  }

  /**
   * Get the taxonomy that describes the {@link org.obiba.mica.network.domain.Network} properties.
   *
   * @return
   */
  @NotNull
  public Taxonomy getNetworkTaxonomy() {
    initialize();
    return networkTaxonomy;
  }

  /**
   * Get the taxonomy that describes the {@link org.obiba.mica.study.domain.BaseStudy} properties.
   *
   * @return
   */
  @NotNull
  public Taxonomy getStudyTaxonomy() {
    initialize();
    return studyTaxonomy;
  }

  /**
   * Get the taxonomy that describes the {@link org.obiba.mica.dataset.domain.Dataset} properties.
   *
   * @return
   */
  @NotNull
  public Taxonomy getDatasetTaxonomy() {
    initialize();
    return datasetTaxonomy;
  }

  /**
   * Get the taxonomy that describes the {@link org.obiba.mica.dataset.domain.DatasetVariable} properties.
   *
   * @return
   */
  @NotNull
  public Taxonomy getVariableTaxonomy() {
    initialize();
    return variableTaxonomy;
  }

  /**
   * Get all taxonomies that apply to the variables, including the one about the built-in properties of the {@link org.obiba.mica.dataset.domain.DatasetVariable}.
   *
   * @return
   */
  @NotNull
  public List<Taxonomy> getAllVariableTaxonomies() {
    return Stream.concat(getVariableTaxonomies().stream(), Stream.of(getVariableTaxonomy())).collect(Collectors.toList());
  }

  /**
   * Get the taxonomies that apply to the variables' annotations.
   *
   * @return
   */
  @NotNull
  public synchronized List<Taxonomy> getVariableTaxonomies() {
    List<Taxonomy> taxonomies = null;
    try {
      taxonomies = variableTaxonomiesService.getTaxonomies();
    } catch (Exception e) {
      // ignore
    }
    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }

  /**
   * Prepare taxonomies for being re-initialized.
   */
  public synchronized void refresh() {
    taxonomyTaxonomy = null;
    networkTaxonomy = null;
    studyTaxonomy = null;
    datasetTaxonomy = null;
    variableTaxonomy = null;
  }

  //
  // Private methods
  //

  private synchronized void initialize() {
    initializeTaxonomyTaxonomy();
    initializeNetworkTaxonomy();
    initializeStudyTaxonomy();
    initializeDatasetTaxonomy();
    initializeVariableTaxonomy();
  }

  private void initializeTaxonomyTaxonomy() {
    if (taxonomyTaxonomy != null) return;
    taxonomyTaxonomy = copy(findTaxonomy(TaxonomyTarget.TAXONOMY));
    MicaConfig config = micaConfigService.getConfig();
    if (!config.isNetworkEnabled() || config.isSingleNetworkEnabled()) {
      taxonomyTaxonomy.removeVocabulary("network");
    }
    if (!config.isStudyDatasetEnabled() && !config.isHarmonizationDatasetEnabled()) {
      taxonomyTaxonomy.removeVocabulary("dataset");
      taxonomyTaxonomy.removeVocabulary("variable");
    }
    if (config.isSingleStudyEnabled() && !config.isHarmonizationDatasetEnabled()) {
      taxonomyTaxonomy.removeVocabulary("study");
    }
  }

  private void initializeNetworkTaxonomy() {
    if (networkTaxonomy != null) return;
    networkTaxonomy = copy(findTaxonomy(TaxonomyTarget.NETWORK));
    networkHelper.applyIdTerms(networkTaxonomy, "id");
    networksSetsAggregationMetaDataHelper.applyIdTerms(networkTaxonomy, "sets");
    studyHelper.applyIdTerms(networkTaxonomy, "studyIds");
  }

  private void initializeStudyTaxonomy() {
    if (studyTaxonomy != null) return;
    studyTaxonomy = copy(findTaxonomy(TaxonomyTarget.STUDY));
    studyHelper.applyIdTerms(studyTaxonomy, "id");
    studiesSetsHelper.applyIdTerms(studyTaxonomy, "sets");
  }

  private void initializeDatasetTaxonomy() {
    if (datasetTaxonomy != null) return;
    datasetTaxonomy = copy(findTaxonomy(TaxonomyTarget.DATASET));
    datasetHelper.applyIdTerms(datasetTaxonomy, "id");
  }

  private void initializeVariableTaxonomy() {
    if (variableTaxonomy != null) return;
    variableTaxonomy = copy(findTaxonomy(TaxonomyTarget.VARIABLE));
    studyHelper.applyIdTerms(variableTaxonomy, "studyId");
    datasetHelper.applyIdTerms(variableTaxonomy, "datasetId");
    populationHelper.applyIdTerms(variableTaxonomy, "populationId");
    dceHelper.applyIdTerms(variableTaxonomy, "dceId");
    variablesSetsHelper.applyIdTerms(variableTaxonomy, "sets");
  }

  private Taxonomy copy(Taxonomy source) {
    Taxonomy target = new Taxonomy();
    BeanUtils.copyProperties(source, target, "vocabularies");
    if (source.hasVocabularies()) {
      source.getVocabularies().forEach(sourceVoc -> {
        Vocabulary targetVoc = new Vocabulary();
        BeanUtils.copyProperties(sourceVoc, targetVoc, "terms");
        if (sourceVoc.hasTerms()) {
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

  /**
   * Check if vocabulary has a term with the given name.
   *
   * @param vocabulary
   * @param name
   * @return
   */
  private Optional<Term> getTerm(Vocabulary vocabulary, String name) {
    if (!vocabulary.hasTerms()) return Optional.empty();
    if (vocabulary.hasTerm(name)) return Optional.of(vocabulary.getTerm(name));
    for (Term t : vocabulary.getTerms()) {
      Optional<Term> res = getTerm(t, name);
      if (res.isPresent()) return res;
    }
    return Optional.empty();
  }

  /**
   * Check if term has a term with the given name.
   *
   * @param term
   * @param name
   * @return
   */
  private Optional<Term> getTerm(Term term, String name) {
    if (!term.hasTerms()) return Optional.empty();
    if (term.hasTerm(name)) return Optional.of(term.getTerm(name));
    for (Term t : term.getTerms()) {
      Optional<Term> res = getTerm(t, name);
      if (res.isPresent()) return res;
    }
    return Optional.empty();
  }

  private Taxonomy findTaxonomy(TaxonomyTarget target) {
    return taxonomyConfigService.findByTarget(target);
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

  @Async
  @Subscribe
  public void documentSetUpdated(DocumentSetUpdatedEvent event) {
    refresh();
  }

  public void refreshTaxonomyTaxonomyIfNeeded(MicaConfig currentConfig, MicaConfig newConfig) {
    if (currentConfig.isSingleStudyEnabled() != newConfig.isSingleStudyEnabled()
      || currentConfig.isNetworkEnabled() != newConfig.isNetworkEnabled()
      || currentConfig.isSingleNetworkEnabled() != newConfig.isSingleNetworkEnabled()
      || currentConfig.isStudyDatasetEnabled() != newConfig.isStudyDatasetEnabled()
      || currentConfig.isHarmonizationDatasetEnabled() != newConfig.isHarmonizationDatasetEnabled()) {

      taxonomyTaxonomy = null;
    }
  }
}
