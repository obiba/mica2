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

import org.junit.Test;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaxonomyConfigServiceTest {

  @Test(expected = VocabularyDuplicateAliasException.class)
  public void validateTaxonomyDuplicateAlias() {
    Taxonomy taxonomy = new Taxonomy("tax001");
    taxonomy.addVocabulary(
      createVocabulary("voc001",
        null,
        AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc").build()));

    taxonomy.addVocabulary(
      createVocabulary("voc002",
        null,
        AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc").build()));

    new TaxonomyConfigService().validateTaxonomy(taxonomy);
  }

  @Test(expected = VocabularyDuplicateAliasException.class)
  public void validateTaxonomyDuplicateAliasRange() {
    Taxonomy taxonomy = new Taxonomy("tax001");
    taxonomy.addVocabulary(
      createVocabulary("voc001",
        createTerms("term001", "term002"),
        AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc-range").range("true").build()));

    taxonomy.addVocabulary(
      createVocabulary("voc002",
        createTerms("term001", "term002"),
        AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc-range").range("true").build()));

    new TaxonomyConfigService().validateTaxonomy(taxonomy);
  }

  @Test
  public void validateRangeTaxonomyWithTerms() {
    Taxonomy taxonomy = new Taxonomy("tax001");
    taxonomy.addVocabulary(
      createVocabulary("voc001",
        createTerms("term001", "term002"),
        AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc-range").range("true").build()));
    new TaxonomyConfigService().validateTaxonomy(taxonomy);
  }

  @Test(expected = VocabularyMissingRangeTermsException.class)
  public void validateRangeTaxonomyWithoutTerms() {
    Taxonomy taxonomy = new Taxonomy("tax001");
    taxonomy.addVocabulary(
      createVocabulary("voc001",
      null,
      AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc-range").range("true").type("integer").build()));
    new TaxonomyConfigService().validateTaxonomy(taxonomy);
  }

  @Test
  public void validateRangeTaxonomyWithoutTermsAndRange() {
    Taxonomy taxonomy = new Taxonomy("tax001");
    taxonomy.addVocabulary(
      createVocabulary("voc001",
      null,
      AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc-range").type("integer").build()));
    new TaxonomyConfigService().validateTaxonomy(taxonomy);
  }

  @Test(expected = VocabularyMissingRangeAttributeException.class)
  public void validateRangeTaxonomyWithoutRangeAttribute() {
    Taxonomy taxonomy = new Taxonomy("tax001");
    taxonomy.addVocabulary(
      createVocabulary("voc001",
        createTerms("term001", "term002"),
        AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc-range").type("integer").build()));
    new TaxonomyConfigService().validateTaxonomy(taxonomy);
  }

  @Test
  public void merge_taxonomy_with_one_having_extra_vocabulary() {
    Taxonomy tax001 = new Taxonomy("tax001");
    Taxonomy tax002 = new Taxonomy("tax002");
    tax002.addVocabulary(createVocabulary("voc002", null, null));
    new TaxonomyConfigService().mergeVocabulariesTerms(tax001, tax002);

    assert tax001.hasVocabulary("voc002");
  }

  @Test
  public void merge_taxonomy_with_one_missing_vocabulary() {
    Taxonomy tax001 = new Taxonomy("tax001");
    tax001.addVocabulary(createVocabulary("voc001", null, null));

    Taxonomy tax002 = new Taxonomy("tax002");
    new TaxonomyConfigService().mergeVocabulariesTerms(tax001, tax002);

    assert tax001.hasVocabulary("voc001");
  }

  @Test
  public void merge_taxonomy_with_one_having_same_vocabulary_but_extra_term() {
    Taxonomy tax001 = new Taxonomy("tax001");
    tax001.addVocabulary(createVocabulary("voc001", null, null));

    Taxonomy tax002 = new Taxonomy("tax002");
    tax002.addVocabulary(createVocabulary("voc001", createTerms("term001"), null));
    new TaxonomyConfigService().mergeVocabulariesTerms(tax001, tax002);

    assert tax001.getVocabulary("voc001").hasTerm("term001");
  }

  @Test
  public void merge_taxonomy_with_vocabulary_having_one_term_with_one_having_same_vocabulary_without_term() {
    Taxonomy tax001 = new Taxonomy("tax001");
    tax001.addVocabulary(createVocabulary("voc001", createTerms("term001"), null));

    Taxonomy tax002 = new Taxonomy("tax002");
    tax002.addVocabulary(createVocabulary("voc001", null, null));
    new TaxonomyConfigService().mergeVocabulariesTerms(tax001, tax002);

    assert tax001.getVocabulary("voc001").hasTerm("term001");
  }

  private Vocabulary createVocabulary(String name, List<Term> terms, Map<String,String> attributes) {
    Vocabulary vocabulary = new Vocabulary(name);
    vocabulary.setTitle(LocalizedString.en(name + "-title"));
    vocabulary.setDescription(LocalizedString.en(name + "-desc"));
    if (terms != null) vocabulary.setTerms(terms);
    if (attributes != null) vocabulary.setAttributes(attributes);
    return vocabulary;
  }

  private List<Term> createTerms(String... names) {
    return Arrays.stream(names).map(n -> createTerm(n, n +"-title", n + "-desc")).collect(Collectors.toList());
  }

  private Term createTerm(String name, String title, String desc) {
    Term term = new Term(name);
    term.setTitle(LocalizedString.en(title));
    term.setDescription(LocalizedString.en(desc));
    return term;
  }

  private static class AttributeBuilder {
    private Map<String, String> attributes = new HashMap<>();

    static AttributeBuilder newBuilder() {
      return new AttributeBuilder();
    }

    AttributeBuilder field(String value) {
      attributes.put("field", value);
      return this;
    }

    AttributeBuilder alias(String value) {
      attributes.put("alias", value);
      return this;
    }

    AttributeBuilder type(String value) {
      attributes.put("type", value);
      return this;
    }

    AttributeBuilder range(String value) {
      attributes.put("range", value);
      return this;
    }

    public Map<String, String> build() {
      return attributes;
    }
  }
}
