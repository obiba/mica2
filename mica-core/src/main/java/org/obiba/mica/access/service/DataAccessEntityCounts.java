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

import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessEntityWithParent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Count of the child entities of a data access request, and of the pending ones (neither approved nor rejected).
 */
public record DataAccessEntityCounts(int total, int pending) {

  public static final DataAccessEntityCounts NONE = new DataAccessEntityCounts(0, 0);

  static Map<String, DataAccessEntityCounts> byParentId(List<? extends DataAccessEntityWithParent> entities) {
    return entities.stream().collect(Collectors.toMap(DataAccessEntityWithParent::getParentId,
      e -> new DataAccessEntityCounts(1, isPending(e) ? 1 : 0),
      (c1, c2) -> new DataAccessEntityCounts(c1.total + c2.total, c1.pending + c2.pending)));
  }

  private static boolean isPending(DataAccessEntityWithParent entity) {
    return entity.getStatus() != DataAccessEntityStatus.APPROVED && entity.getStatus() != DataAccessEntityStatus.REJECTED;
  }
}
