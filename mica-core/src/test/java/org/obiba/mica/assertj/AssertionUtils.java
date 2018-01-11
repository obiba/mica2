/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.assertj;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertionUtils {

  private AssertionUtils() {}

  public static <T> void areIterableFieldsEqualToEachOther(Iterable<T> actualCollection,
      Iterable<T> expectedCollection) {

    assertThat(actualCollection).hasSameSizeAs(expectedCollection);
    Iterator<T> actualIt = actualCollection.iterator();
    Iterator<T> expectedIt = expectedCollection.iterator();
    while(actualIt.hasNext()) {
      assertThat(actualIt.next()).isEqualToComparingFieldByField(expectedIt.next());
    }
  }

}
