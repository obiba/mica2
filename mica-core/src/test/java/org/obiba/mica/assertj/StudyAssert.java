package org.obiba.mica.assertj;

import org.assertj.core.api.AbstractAssert;
import org.obiba.mica.domain.Study;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import static org.assertj.core.api.Assertions.assertThat;

public class StudyAssert extends AbstractAssert<StudyAssert, Study> {

  protected StudyAssert(Study actual) {
    super(actual, StudyAssert.class);
  }

  public StudyAssert areFieldsEqualToEachOther(Study expected) {
    JsonElement actualJson = new GsonBuilder().create().toJsonTree(actual, actual.getClass());
    JsonElement expectedJson = new GsonBuilder().create().toJsonTree(expected, expected.getClass());
    assertThat(actualJson).isEqualTo(expectedJson);
    return myself;
  }
}
