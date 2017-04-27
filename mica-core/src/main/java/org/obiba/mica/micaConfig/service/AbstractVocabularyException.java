package org.obiba.mica.micaConfig.service;

import org.obiba.opal.core.domain.taxonomy.Vocabulary;

public class AbstractVocabularyException extends RuntimeException {

  private Vocabulary vocabulary;

  public AbstractVocabularyException() {
    super();
  }

  public AbstractVocabularyException(String message, Vocabulary v) {
    super(message);
    vocabulary = v;
  }

  public Vocabulary getVocabulary() {
    return vocabulary;
  }
}
