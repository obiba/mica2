/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access;

import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.core.repository.DBRefAwareRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface DataAccessRequestRepositoryCustom extends DBRefAwareRepository<DataAccessRequest> {
  Map<Object, LinkedHashMap> getAllAmendmentsSummary();
  Map<Object, LinkedHashMap> getAmendmentsSummary(String id);
  List<LinkedHashMap> getCountByStatus();
}
