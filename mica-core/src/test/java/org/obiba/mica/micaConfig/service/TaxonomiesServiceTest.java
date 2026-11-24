/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import org.junit.jupiter.api.Test;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.helper.DatasetIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.DceIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.NetworkIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.NetworksSetsAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.PopulationIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.StudiesSetsAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.StudyIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.VariablesSetsAggregationMetaDataHelper;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaxonomiesServiceTest {

  @Test
  public void variableSiblingsAreTheTermsOfTheGroupThatHasTheTaxonomy() {
    Vocabulary variable = variableVocabulary();

    assertEquals(List.of("Mlstr_area", "Mica_variable"),
      names(TaxonomiesService.taxonomySiblings(TaxonomyTarget.VARIABLE, variable, "Mica_variable")));
    assertEquals(List.of("Mlstr_habits", "Mlstr_social"),
      names(TaxonomiesService.taxonomySiblings(TaxonomyTarget.VARIABLE, variable, "Mlstr_social")));
    assertTrue(TaxonomiesService.taxonomySiblings(TaxonomyTarget.VARIABLE, variable, "Unknown").isEmpty());
  }

  @Test
  public void otherTargetSiblingsAreTheVocabularyTerms() {
    Vocabulary study = new Vocabulary("study");
    study.addTerm(new Term("Mica_study"));

    assertEquals(List.of("Mica_study"),
      names(TaxonomiesService.taxonomySiblings(TaxonomyTarget.STUDY, study, "Mica_study")));
    assertTrue(TaxonomiesService.taxonomySiblings(TaxonomyTarget.STUDY, study, "Unknown").isEmpty());
    assertTrue(TaxonomiesService.taxonomySiblings(TaxonomyTarget.NETWORK, new Vocabulary("network"), "Mica_network").isEmpty());
  }

  @Test
  public void moveTermWithinASecondGroup() {
    Vocabulary variable = variableVocabulary();
    List<Term> scales = TaxonomiesService.taxonomySiblings(TaxonomyTarget.VARIABLE, variable, "Mlstr_social");

    assertTrue(TaxonomiesService.moveTerm(scales, "Mlstr_social", true));
    assertEquals(List.of("Mlstr_social", "Mlstr_habits"), names(variable.getTerm("Scales").getTerms()));
    assertEquals(List.of("Mlstr_area", "Mica_variable"), names(variable.getTerm("Variable_chars").getTerms()));
  }

  @Test
  public void moveTermOutOfBoundsOrUnknownDoesNothing() {
    Vocabulary variable = variableVocabulary();
    List<Term> chars = variable.getTerm("Variable_chars").getTerms();

    assertFalse(TaxonomiesService.moveTerm(chars, "Mlstr_area", true));
    assertFalse(TaxonomiesService.moveTerm(chars, "Mica_variable", false));
    assertFalse(TaxonomiesService.moveTerm(chars, "Mlstr_social", true));
    assertEquals(List.of("Mlstr_area", "Mica_variable"), names(chars));
  }

  @Test
  public void setAttributeDoesNotSaveTheTaxonomiesHiddenByTheConfig() {
    MetaTaxonomyStore store = new MetaTaxonomyStore();
    TaxonomiesService service = service(store, networksDisabled());
    assertEquals("true", service.getTaxonomyTaxonomy().getVocabulary("network").getTerm("Mica_network").getAttributeValue("hidden"));

    service.setTaxonomyAttribute(TaxonomyTarget.VARIABLE, "Mlstr_area", "hidden", "true");

    Taxonomy saved = store.saved;
    assertEquals("true", variableTerm(saved, "Mlstr_area").getAttributeValue("hidden"));
    assertNull(saved.getVocabulary("network").getTerm("Mica_network").getAttributeValue("hidden"));
    // the in-memory one has the change and the config overlay
    Taxonomy meta = service.getTaxonomyTaxonomy();
    assertEquals("true", variableTerm(meta, "Mlstr_area").getAttributeValue("hidden"));
    assertEquals("true", meta.getVocabulary("network").getTerm("Mica_network").getAttributeValue("hidden"));
  }

  @Test
  public void moveDoesNotSaveTheTaxonomiesHiddenByTheConfig() {
    MetaTaxonomyStore store = new MetaTaxonomyStore();
    TaxonomiesService service = service(store, networksDisabled());

    service.moveTaxonomy(TaxonomyTarget.VARIABLE, "Mlstr_area", true);

    assertEquals(List.of("Mlstr_area", "Mica_variable"),
      names(store.saved.getVocabulary("variable").getTerm("Variable_chars").getTerms()));
    assertNull(store.saved.getVocabulary("network").getTerm("Mica_network").getAttributeValue("hidden"));
    assertEquals(List.of("Mlstr_area", "Mica_variable"),
      names(service.getTaxonomyTaxonomy().getVocabulary("variable").getTerm("Variable_chars").getTerms()));
  }

  @Test
  public void unknownTaxonomyIsNotSaved() {
    MetaTaxonomyStore store = new MetaTaxonomyStore();
    TaxonomiesService service = service(store, networksDisabled());

    service.setTaxonomyAttribute(TaxonomyTarget.VARIABLE, "Unknown", "hidden", "true");
    service.moveTaxonomy(TaxonomyTarget.STUDY, "Mica_study", true);

    assertNull(store.saved);
  }

  private static MicaConfig networksDisabled() {
    MicaConfig config = new MicaConfig();
    config.setNetworkEnabled(false);
    return config;
  }

  private static Term variableTerm(Taxonomy meta, String name) {
    return meta.getVocabulary("variable").getTerm("Variable_chars").getTerm(name);
  }

  /**
   * The stored meta-taxonomy: each read is a new instance, as from the database.
   */
  private static class MetaTaxonomyStore {
    Taxonomy saved;

    Taxonomy read() {
      Taxonomy meta = new Taxonomy("Mica_taxonomy");
      Vocabulary network = new Vocabulary("network");
      network.addTerm(new Term("Mica_network"));
      Vocabulary study = new Vocabulary("study");
      study.addTerm(new Term("Mica_study"));
      Vocabulary variable = new Vocabulary("variable");
      variable.addTerm(group("Variable_chars", "Mica_variable", "Mlstr_area"));
      meta.addVocabulary(network);
      meta.addVocabulary(study);
      meta.addVocabulary(variable);
      if (saved != null) {
        // replays the saved order and attributes
        Term chars = variable.getTerm("Variable_chars");
        chars.setTerms(saved.getVocabulary("variable").getTerm("Variable_chars").getTerms().stream()
          .map(term -> {
            Term copy = new Term(term.getName());
            copy.setAttributes(new HashMap<>(term.getAttributes()));
            return copy;
          })
          .collect(Collectors.toList()));
        network.getTerm("Mica_network").setAttributes(
          new HashMap<>(saved.getVocabulary("network").getTerm("Mica_network").getAttributes()));
      }
      return meta;
    }
  }

  private static TaxonomiesService service(MetaTaxonomyStore store, MicaConfig config) {
    MicaConfigService micaConfigService = mock(MicaConfigService.class);
    when(micaConfigService.getConfig()).thenReturn(config);
    VariableTaxonomiesService variableTaxonomiesService = mock(VariableTaxonomiesService.class);
    when(variableTaxonomiesService.getTaxonomies()).thenReturn(List.of(new Taxonomy("Mlstr_area")));
    TaxonomyConfigService taxonomyConfigService = mock(TaxonomyConfigService.class);
    when(taxonomyConfigService.findByTarget(any())).thenAnswer(invocation -> {
      TaxonomyTarget target = invocation.getArgument(0);
      return TaxonomyTarget.TAXONOMY.equals(target) ? store.read() : new Taxonomy("Mica_" + target.asId());
    });
    doAnswer(invocation -> {
      store.saved = invocation.getArgument(1);
      return null;
    }).when(taxonomyConfigService).update(eq(TaxonomyTarget.TAXONOMY), any());
    return new TaxonomiesService(variableTaxonomiesService, micaConfigService,
      mock(StudyIdAggregationMetaDataHelper.class), mock(DatasetIdAggregationMetaDataHelper.class),
      mock(NetworkIdAggregationMetaDataHelper.class), mock(PopulationIdAggregationMetaDataHelper.class),
      mock(DceIdAggregationMetaDataHelper.class), mock(NetworksSetsAggregationMetaDataHelper.class),
      mock(StudiesSetsAggregationMetaDataHelper.class), mock(VariablesSetsAggregationMetaDataHelper.class),
      taxonomyConfigService);
  }

  private static Vocabulary variableVocabulary() {
    Vocabulary variable = new Vocabulary("variable");
    variable.addTerm(group("Variable_chars", "Mlstr_area", "Mica_variable"));
    variable.addTerm(group("Scales", "Mlstr_habits", "Mlstr_social"));
    return variable;
  }

  private static Term group(String name, String... taxonomies) {
    Term group = new Term(name);
    group.setTerms(Arrays.stream(taxonomies).map(Term::new).collect(Collectors.toList()));
    return group;
  }

  private static List<String> names(List<Term> terms) {
    return terms.stream().map(Term::getName).collect(Collectors.toList());
  }
}
