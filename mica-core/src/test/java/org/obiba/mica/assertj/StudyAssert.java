package org.obiba.mica.assertj;

import java.util.Iterator;

import org.assertj.core.api.AbstractAssert;
import org.obiba.mica.domain.Population;
import org.obiba.mica.domain.Study;

import com.google.common.collect.ObjectArrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.obiba.mica.assertj.AssertionUtils.areIterableFieldsEqualToEachOther;

public class StudyAssert extends AbstractAssert<StudyAssert, Study> {

  private static final String[] DEFAULT_EXCLUDES = new String[] { "contacts", "investigators", "populations" };

  protected StudyAssert(Study actual) {
    super(actual, StudyAssert.class);
  }

  public StudyAssert areFieldsEqualToEachOther(Study expected, String... excludeFields) {
    assertThat(actual).isEqualTo(expected);

    String[] excludes = excludeFields == null
        ? DEFAULT_EXCLUDES
        : ObjectArrays.concat(DEFAULT_EXCLUDES, excludeFields, String.class);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, excludes);
    areIterableFieldsEqualToEachOther(actual.getContacts(), expected.getContacts());
    areIterableFieldsEqualToEachOther(actual.getInvestigators(), expected.getInvestigators());
    arePopulationFieldsEqualToEachOther(actual.getPopulations(), expected.getPopulations());
    return myself;
  }

  private void arePopulationFieldsEqualToEachOther(Iterable<Population> actualPops, Iterable<Population> expectedPops) {
    assertThat(actualPops).hasSameSizeAs(expectedPops);
    Iterator<Population> actualIt = actualPops.iterator();
    Iterator<Population> expectedIt = expectedPops.iterator();
    while(actualIt.hasNext()) {
      Population actualPop = actualIt.next();
      Population expectedPop = expectedIt.next();
      assertThat(actualPop).isEqualToIgnoringGivenFields(expectedPop, "dataCollectionEvents");
      areIterableFieldsEqualToEachOther(actualPop.getDataCollectionEvents(), expectedPop.getDataCollectionEvents());
    }
  }

}
