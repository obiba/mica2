package org.obiba.mica.assertj;

import org.obiba.mica.study.domain.Study;

public class Assertions extends org.assertj.core.api.Assertions {

  public static StudyAssert assertThat(Study actual) {
    return new StudyAssert(actual);
  }

}
