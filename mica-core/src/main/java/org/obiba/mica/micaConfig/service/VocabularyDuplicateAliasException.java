package org.obiba.mica.micaConfig.service;

import org.obiba.opal.core.domain.taxonomy.Vocabulary;

public class VocabularyDuplicateAliasException extends AbstractVocabularyException {

  private static final long serialVersionUID = 1184806332897637307L;

  public VocabularyDuplicateAliasException() {
    super();
  }

  public VocabularyDuplicateAliasException(Vocabulary v) {
    super("Duplicate vocabulary alias.", v);
  }
}
