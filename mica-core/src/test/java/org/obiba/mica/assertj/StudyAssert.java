package org.obiba.mica.assertj;

import org.assertj.core.api.AbstractAssert;
import org.obiba.mica.domain.Study;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import static org.assertj.core.api.Assertions.assertThat;

public class StudyAssert extends AbstractAssert<StudyAssert, Study> {

  private final Gson gson = new GsonBuilder().create();

  protected StudyAssert(Study actual) {
    super(actual, StudyAssert.class);
  }

  public StudyAssert areFieldsEqualToEachOther(Study expected) {
    JsonElement actualJson = gson.toJsonTree(actual, actual.getClass());
    JsonElement expectedJson = gson.toJsonTree(expected, expected.getClass());
    assertThat(actualJson).isEqualTo(expectedJson);
    return myself;
  }
}
