package org.obiba.mica.micaConfig.service;

import org.obiba.opal.core.domain.taxonomy.Vocabulary;

public class VocabularyMissingRangeAttributeException extends AbstractVocabularyException {

  private static final long serialVersionUID = 3925364191872315281L;

  public VocabularyMissingRangeAttributeException() {
    super();
  }

  public VocabularyMissingRangeAttributeException(Vocabulary v) {
    super(String.format("The vocabulary '%s' with terms is missing a range attribute.", v.getName()), v);
  }
}
