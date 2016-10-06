package org.obiba.mica.search.csvexport.translator;

public class EmptyTranslator implements Translator {

  @Override
  public String translate(String key) {
    return key;
  }
}
