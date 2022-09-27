/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access;

import org.obiba.mica.access.domain.DataAccessAgreement;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * Spring Data MongoDB repository for the {@link DataAccessAgreement} entity.
 */

public interface DataAccessAgreementRepository extends DataAccessEntityRepository<DataAccessAgreement> {

  List<DataAccessAgreement> findByParentId(String parentId);

  int countByParentId(String parentId);

  @Query(value = "{ parentId: ?0, status: { $nin: [\"APPROVED\", \"REJECTED\"] } }", count = true)
  int countPendingByParentId(String parentId);

}
