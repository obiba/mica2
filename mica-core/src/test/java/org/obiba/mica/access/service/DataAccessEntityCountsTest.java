/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.service;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.obiba.mica.access.domain.DataAccessAgreement;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessEntityWithParent;

import static org.assertj.core.api.Assertions.assertThat;

public class DataAccessEntityCountsTest {

  @Test
  public void test_byParentId_counts_total_and_pending_per_parent() {
    Map<String, DataAccessEntityCounts> counts = DataAccessEntityCounts.byParentId(List.of(
      amendment("dar1", DataAccessEntityStatus.OPENED),
      amendment("dar1", DataAccessEntityStatus.APPROVED),
      amendment("dar1", DataAccessEntityStatus.SUBMITTED),
      amendment("dar2", DataAccessEntityStatus.REJECTED)));

    assertThat(counts).containsOnlyKeys("dar1", "dar2");
    assertThat(counts.get("dar1")).isEqualTo(new DataAccessEntityCounts(3, 2));
    assertThat(counts.get("dar2")).isEqualTo(new DataAccessEntityCounts(1, 0));
  }

  /**
   * Pending is any status but approved or rejected, as in the countPendingByParentId repository queries.
   */
  @Test
  public void test_byParentId_pending_is_neither_approved_nor_rejected() {
    for (DataAccessEntityStatus status : DataAccessEntityStatus.values()) {
      boolean pending = status != DataAccessEntityStatus.APPROVED && status != DataAccessEntityStatus.REJECTED;
      assertThat(DataAccessEntityCounts.byParentId(List.of(agreement("dar1", status))).get("dar1"))
        .as(status.name())
        .isEqualTo(new DataAccessEntityCounts(1, pending ? 1 : 0));
    }
  }

  @Test
  public void test_byParentId_no_entities() {
    assertThat(DataAccessEntityCounts.byParentId(List.of())).isEmpty();
  }

  private static DataAccessEntityWithParent amendment(String parentId, DataAccessEntityStatus status) {
    return (DataAccessEntityWithParent) DataAccessAmendment.newBuilder().parentId(parentId).status(status.name()).build();
  }

  private static DataAccessEntityWithParent agreement(String parentId, DataAccessEntityStatus status) {
    return (DataAccessEntityWithParent) DataAccessAgreement.newBuilder().parentId(parentId).status(status.name()).build();
  }
}
