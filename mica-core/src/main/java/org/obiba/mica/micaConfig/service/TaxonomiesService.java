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
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.event.DocumentSetUpdatedEvent;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUnpublishedEvent;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.helper.DatasetIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.DceIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.NetworkIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.NetworksSetsAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.PopulationIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.StudiesSetsAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.StudyIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.VariablesSetsAggregationMetaDataHelper;
import org.obiba.mica.network.event.NetworkPublishedEvent;
import org.obiba.mica.network.event.NetworkUnpublishedEvent;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.mica.study.event.StudyUnpublishedEvent;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TaxonomiesService {

  private static final Logger log = LoggerFactory.getLogger(TaxonomiesService.class);

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

  public static List<Taxonomy> processMergedAttributes(List<Taxonomy> variableTaxonomies, Map<String, Map<String, List<LocalizedString>>> groupedAttributes) {
    List<Taxonomy> taxonomies = new ArrayList<>();

    groupedAttributes.keySet().forEach(taxonomyName -> {
      Taxonomy foundTaxonomy = variableTaxonomies.stream().filter(taxonomy -> taxonomy.getName().equals(taxonomyName)).findFirst().orElse(null);
      if (foundTaxonomy != null) {
        Set<String> vocabularyNames = groupedAttributes.get(taxonomyName).keySet();
        List<Vocabulary> foundVocabularies = foundTaxonomy.getVocabularies().stream().filter(vocabulary -> vocabularyNames.contains(vocabulary.getName())).collect(Collectors.toList());

        Taxonomy theTaxonomy = new Taxonomy(foundTaxonomy.getName());
        theTaxonomy.setTitle(foundTaxonomy.getTitle());
        theTaxonomy.setDescription(foundTaxonomy.getDescription());

        foundVocabularies.forEach(vocabulary -> {
          Vocabulary aVocabulary = new Vocabulary(vocabulary.getName());
          aVocabulary.setTitle(vocabulary.getTitle());
          aVocabulary.setDescription(vocabulary.getDescription());

          List<String> termNames = groupedAttributes.get(taxonomyName).get(vocabulary.getName()).stream().map(LocalizedString::getUndetermined).collect(Collectors.toList());

          aVocabulary.setTerms(vocabulary.getTerms().stream().filter(term -> termNames.contains(term.getName())).collect(Collectors.toList()));

          theTaxonomy.addVocabulary(aVocabulary);
        });

        taxonomies.add(theTaxonomy);
      }
    });

    return taxonomies;
  }

  @NotNull
  public Taxonomy getTaxonomyTaxonomy() {
    initialize();
    Taxonomy meta = taxonomyTaxonomy;
    List<Taxonomy> variableTaxonomies = getAllVariableTaxonomies();
    // the in-memory one is checked first, so that the stored one is read only when there is something to save
    if (syncVariableTaxonomies(meta, variableTaxonomies)) {
      saveSyncedMetaTaxonomy(variableTaxonomies);
    }
    return meta;
  }

  /**
   * Save the variable taxonomies changes to the stored meta-taxonomy, not to the in-memory one: the latter has the
   * taxonomies hidden by the Mica config (see {@link #initializeTaxonomyTaxonomy()}), which must not be saved. The
   * in-memory one is then rebuilt from the saved one.
   *
   * @param variableTaxonomies
   */
  private synchronized void saveSyncedMetaTaxonomy(List<Taxonomy> variableTaxonomies) {
    Taxonomy metaTaxonomy = findTaxonomy(TaxonomyTarget.TAXONOMY);
    // a concurrent call may have saved it already
    if (syncVariableTaxonomies(metaTaxonomy, variableTaxonomies)) {
      log.debug("Taxonomy of taxonomies was modified");
      this.taxonomyTaxonomy = null;
      taxonomyConfigService.update(TaxonomyTarget.TAXONOMY, metaTaxonomy);
    }
  }

  /**
   * Add the new variable taxonomies to the meta-taxonomy (hidden), update their title and description, and hide the
   * ones that do not exist anymore.
   *
   * @param meta
   * @param variableTaxonomies
   * @return Whether the meta-taxonomy was modified
   */
  static boolean syncVariableTaxonomies(Taxonomy meta, List<Taxonomy> variableTaxonomies) {
    boolean modified = false;
    for (Vocabulary vocabulary : meta.getVocabularies()) {

      if (vocabulary.getName().equals("variable")) {
        Term variableChars = vocabulary.getTerm("Variable_chars");
        Set<String> termsInOtherGroups = vocabulary.getTerms().stream()
          .filter(term -> !term.equals(variableChars) && term.hasTerms())
          .flatMap(term -> term.getTerms().stream())
          .map(Term::getName)
          .collect(Collectors.toSet());

        // check variable taxonomies to be added to meta
        List<String> variableTaxonomiesNames = variableChars.getTerms().stream()
          .map(TaxonomyEntity::getName).collect(Collectors.toList());
        for (Taxonomy variableTaxonomy : variableTaxonomies) {
          String variableTaxonomyName = variableTaxonomy.getName();
          if (!variableTaxonomiesNames.contains(variableTaxonomyName) && !termsInOtherGroups.contains(variableTaxonomyName)) {
            Term newTerm = new Term(variableTaxonomy.getName());
            newTerm.addAttribute("hidden", "true");
            newTerm.setTitle(variableTaxonomy.getTitle());
            newTerm.setDescription(variableTaxonomy.getDescription());
            variableChars.getTerms().add(newTerm);
            modified = true;
          } else if (!termsInOtherGroups.contains(variableTaxonomyName)) {
            // check any title/description modifications
            Term term = variableChars.getTerm(variableTaxonomy.getName());
            MapDifference<String, String> diff = Maps.difference(term.getTitle(), variableTaxonomy.getTitle());
            if (!diff.areEqual()) {
              term.setTitle(variableTaxonomy.getTitle());
              modified = true;
            }
            diff = Maps.difference(term.getDescription(), variableTaxonomy.getDescription());
            if (!diff.areEqual()) {
              term.setDescription(variableTaxonomy.getDescription());
              modified = true;
            }
          }
        }

        // check variable taxonomies to be hidden in meta
        List<String> reverseVariableTaxonomiesNames = variableTaxonomies.stream()
          .map(TaxonomyEntity::getName).collect(Collectors.toList());
        List<Term> termsToHide = variableChars.getTerms().stream()
          .filter(term -> !"Mica_variable".equals(term.getName()) && !reverseVariableTaxonomiesNames.contains(term.getName()))
          .filter(term -> term.getAttributeValue("hidden") == null || term.getAttributeValue("hidden").equals("false"))
          .collect(Collectors.toList());

        if (!termsToHide.isEmpty()) {
          for (Term term : termsToHide) {
            term.addAttribute("hidden", "true");
          }
          modified = true;
        }
      }
    }
    return modified;
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
   * Change described taxonomy position when there are several for the target considered.
   *
   * @param target
   * @param name
   * @param up
   */
  public void moveTaxonomy(TaxonomyTarget target, String name, boolean up) {
    updateMetaTaxonomy(target, name, siblings -> moveTerm(siblings, name, up));
  }

  /**
   * Apply a change to the stored meta-taxonomy, not to the in-memory one: the latter has the taxonomies hidden by the
   * Mica config (see {@link #initializeTaxonomyTaxonomy()}), which must not be saved. The in-memory one is then
   * rebuilt from the saved one.
   *
   * @param target
   * @param taxonomyName
   * @param change Applied to the siblings of the taxonomy, returns whether they were modified
   */
  private synchronized void updateMetaTaxonomy(TaxonomyTarget target, String taxonomyName, Predicate<List<Term>> change) {
    // saves the newly found variable taxonomies first
    getTaxonomyTaxonomy();
    Taxonomy metaTaxonomy = findTaxonomy(TaxonomyTarget.TAXONOMY);
    boolean modified = false;
    for (Vocabulary vocabulary : metaTaxonomy.getVocabularies()) {
      if (vocabulary.getName().equals(target.asId())) {
        modified = change.test(taxonomySiblings(target, vocabulary, taxonomyName));
      }
    }
    if (modified) {
      // rebuilt from the saved one, by the update event listeners as well
      this.taxonomyTaxonomy = null;
      taxonomyConfigService.update(TaxonomyTarget.TAXONOMY, metaTaxonomy);
    }
  }

  /**
   * The terms of the meta-taxonomy target vocabulary that include the described taxonomy: for the variable
   * target, the terms of the group (e.g. Variable_chars, Scales) that has it, otherwise the vocabulary terms.
   *
   * @param target
   * @param vocabulary
   * @param taxonomyName
   * @return The sibling terms, including the taxonomy one, or an empty list if not found
   */
  static List<Term> taxonomySiblings(TaxonomyTarget target, Vocabulary vocabulary, String taxonomyName) {
    if (!vocabulary.hasTerms()) return Collections.emptyList();
    if (TaxonomyTarget.VARIABLE.equals(target)) {
      return vocabulary.getTerms().stream()
        .filter(group -> group.hasTerms() && group.hasTerm(taxonomyName))
        .map(Term::getTerms)
        .findFirst()
        .orElse(Collections.emptyList());
    }
    return vocabulary.hasTerm(taxonomyName) ? vocabulary.getTerms() : Collections.emptyList();
  }

  /**
   * Modify term list by moving up/down the term with provided name.
   *
   * @param terms
   * @param taxonomyName
   * @param up
   * @return Whether the list was modified
   */
  static boolean moveTerm(List<Term> terms, String taxonomyName, boolean up) {
    int idx = -1;
    for (int i = 0; i < terms.size(); i++) {
      if (terms.get(i).getName().equals(taxonomyName)) {
        idx = i;
        break;
      }
    }
    if (idx > -1) {
      int newIdx = up ? idx - 1 : idx + 1;
      if (newIdx > -1 && newIdx < terms.size()) {
        Term term = terms.remove(idx);
        terms.add(newIdx, term);
        return true;
      }
    }
    return false;
  }

  /**
   * Hide/show the described taxonomy for the target considered.
   *
   * @param target
   * @param taxonomyName
   * @param name
   * @param value
   */
  public void setTaxonomyAttribute(TaxonomyTarget target, String taxonomyName, String name, String value) {
    if (Strings.isNullOrEmpty(name)) return;
    updateMetaTaxonomy(target, taxonomyName, siblings -> {
      Optional<Term> found = siblings.stream().filter(term -> term.getName().equals(taxonomyName)).findFirst();
      found.ifPresent(term -> term.getAttributes().put(name, value));
      return found.isPresent();
    });
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
    if (taxonomyTaxonomy == null)
      taxonomyTaxonomy = copy(findTaxonomy(TaxonomyTarget.TAXONOMY));
    MicaConfig config = micaConfigService.getConfig();
    if (!config.isNetworkEnabled() || config.isSingleNetworkEnabled()) {
      hideMetaVocabularyTerms("network");
    }
    if (!config.isStudyDatasetEnabled() && !config.isHarmonizationDatasetEnabled()) {
      hideMetaVocabularyTerms("dataset");
      hideMetaVocabularyTerms("variable");
    }
    if (config.isSingleStudyEnabled() && !config.isHarmonizationDatasetEnabled()) {
      hideMetaVocabularyTerms("study");
    }
  }

  private void hideMetaVocabularyTerms(String vocabularyName) {
    if (taxonomyTaxonomy.hasVocabulary(vocabularyName)) {
      Vocabulary vocabulary = taxonomyTaxonomy.getVocabulary(vocabularyName);
      if (TaxonomyTarget.VARIABLE.asId().equals(vocabularyName)) {
        Term variableChars = vocabulary.getTerm("Variable_chars");
        if (variableChars.hasTerms())
          variableChars.getTerms().forEach(term -> term.addAttribute("hidden", "true"));
      }
      else if (vocabulary.hasTerms()) {
        vocabulary.getTerms().forEach(term -> term.addAttribute("hidden", "true"));
      }
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
