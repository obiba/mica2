package org.obiba.mica.search.csvexport.translator;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

public class JsonTranslator implements Translator {

  private DocumentContext translationContext;

  public JsonTranslator(String translationsAsJson) {
    translationContext = JsonPath.parse(translationsAsJson);
  }

  @Override
  public String translate(String key) {
    try {
      return translationContext.read(key);
    } catch (PathNotFoundException e) {
      return key;
    }
  }
}
