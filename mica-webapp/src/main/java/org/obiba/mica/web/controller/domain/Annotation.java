package org.obiba.mica.web.controller.domain;

import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;

public class Annotation {

  private final Attribute attribute;

  private final Taxonomy taxonomy;

  public Annotation(Attribute attr, Taxonomy taxonomy) {
    this.attribute = attr;
    this.taxonomy = taxonomy;
  }

  public String getTaxonomyName() {
    return attribute.getNamespace();
  }

  public LocalizedString getTaxonomyTitle() {
    return LocalizedString.from(taxonomy.getTitle());
  }

  public LocalizedString getTaxonomyDescription() {
    return LocalizedString.from(taxonomy.getDescription());
  }

  public String getVocabularyName() {
    return attribute.getName();
  }

  public LocalizedString getVocabularyTitle() {
    return LocalizedString.from(getVocabulary().getTitle());
  }

  public LocalizedString getVocabularyDescription() {
    return LocalizedString.from(getVocabulary().getDescription());
  }

  public String getTermName() {
    return attribute.getValues().get("und");
  }

  public LocalizedString getTermTitle() {
    Vocabulary vocabulary = getVocabulary();
    if (vocabulary.hasTerms() && vocabulary.hasTerm(getTermName())) {
      Term term = vocabulary.getTerm(getTermName());
      return LocalizedString.from(term.getTitle());
    }
    return attribute.getValues();
  }

  public LocalizedString getTermDescription() {
    Vocabulary vocabulary = getVocabulary();
    if (vocabulary.hasTerms() && vocabulary.hasTerm(getTermName())) {
      Term term = vocabulary.getTerm(getTermName());
      return LocalizedString.from(term.getDescription());
    }
    return null;
  }

  private Vocabulary getVocabulary() {
    return taxonomy.getVocabulary(attribute.getName());
  }
}
