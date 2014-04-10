package org.obiba.mica.assertj;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Failures;
import org.assertj.core.util.VisibleForTesting;
import org.obiba.mica.domain.Study;

import static org.assertj.core.api.Assertions.assertThat;

public class StudyAssert extends AbstractAssert<StudyAssert, Study> {

  @VisibleForTesting
  Failures failures = Failures.instance();

  protected StudyAssert(Study actual) {
    super(actual, StudyAssert.class);
  }

  public StudyAssert areFieldsEqualToEachOther(Study expected) {
    assertThat(actual).isEqualTo(expected);
    assertThat(actual).isEqualToComparingFieldByField(expected);
    //TODO compare contact
    //TODO compare investigators
    //TODO compare populations
    //TODO compare data collection events
    return myself;
  }
}
