package org.obiba.mica.micaConfig.service;

import org.obiba.opal.core.domain.taxonomy.Vocabulary;

public class VocabularyMissingRangeTermsException extends AbstractVocabularyException {

  private static final long serialVersionUID = 2011996222183229317L;

  public VocabularyMissingRangeTermsException() {
    super();
  }

  public VocabularyMissingRangeTermsException(Vocabulary v) {
    super(String.format("The vocabulary '%s' having a range attribute is missing terms.", v.getName()), v);
  }
}
