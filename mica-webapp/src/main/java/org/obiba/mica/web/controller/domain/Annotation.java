package org.obiba.mica.web.controller.domain;

import net.sf.cglib.core.Local;
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

  public LocalizedString getTermTitle() {
    Term term = getVocabulary().getTerm(attribute.getValues().get("und"));
    return LocalizedString.from(term.getTitle());
  }

  public LocalizedString getTermDescription() {
    Term term = getVocabulary().getTerm(attribute.getValues().get("und"));
    return LocalizedString.from(term.getDescription());
  }

  private Vocabulary getVocabulary() {
    return taxonomy.getVocabulary(attribute.getName());
  }
}
