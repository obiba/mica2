/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.domain;

import org.junit.jupiter.api.Test;
import org.obiba.mica.core.support.SpecialCharCodecFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DatasetVariableTest {

  @Test
  public void test_id_resolver() throws Exception {
    checkIdResolver("53c3ef8804a61f0e17f6fe78:LAB_TSC:Dataschema", "53c3ef8804a61f0e17f6fe78", "LAB_TSC",
        DatasetVariable.Type.Dataschema, null);
    checkIdResolver("53c3ef8804a61f0e17f6fe78:LAB_TSC:Collected", "53c3ef8804a61f0e17f6fe78", "LAB_TSC",
        DatasetVariable.Type.Collected, null);
    checkIdResolver("53c3ef8804a61f0e17f6fe78:LAB_TSC:Harmonized:Study:53c3ef8704a61f0e17f6fe72", "53c3ef8804a61f0e17f6fe78", "LAB_TSC",
        DatasetVariable.Type.Harmonized, "53c3ef8704a61f0e17f6fe72");
  }

  @Test
  public void test_id_resolver_empty_id() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> DatasetVariable.IdResolver.from(""));
  }

  @Test
  public void test_id_resolver_null_id() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> DatasetVariable.IdResolver.from(null));
  }

  @Test
  public void test_id_resolver_invalid_type() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> DatasetVariable.IdResolver.from("53c3ef8804a61f0e17f6fe78:LAB_TSC:pwel"));
  }

  @Test
  public void test_id_encoderDecoder() {
    String source = "53c3ef8804a61f0e17f6fe78:LAB_TSC (Month) <year>,a=b|2&2:Dataschema";
    String expected = "53c3ef8804a61f0e17f6fe78:LAB_TSC rqlx28rqlMonthrqlx29rql rqlx3crqlyearrqlx3erqlrqlx2crqlarqlx3drqlbrqlx7crql2rqlx26rql2:Dataschema";
    DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(source);
    assertThat(resolver.getId()).isEqualTo(expected);
    String decoded = SpecialCharCodecFactory.get().decode(expected);
    assertThat(decoded).isEqualTo(source);
    assertThat(resolver.getName()).isEqualTo("LAB_TSC (Month) <year>,a=b|2&2");
  }

  private void checkIdResolver(String id, String expectedDatasetId, String expectedName,
      DatasetVariable.Type expectedType, String expectedStudyId) {
    DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(id);
    assertThat(resolver.getDatasetId()).isEqualTo(expectedDatasetId);
    assertThat(SpecialCharCodecFactory.get().decode(resolver.getName())).isEqualTo(expectedName);
    assertThat(resolver.getType()).isEqualTo(expectedType);
    if(expectedStudyId == null) {
      assertThat(resolver.getStudyId()).isNull();
      assertThat(resolver.hasStudyId()).isFalse();
    } else {
      assertThat(resolver.getStudyId()).isEqualTo(expectedStudyId);
      assertThat(resolver.hasStudyId()).isTrue();
    }
  }

}
