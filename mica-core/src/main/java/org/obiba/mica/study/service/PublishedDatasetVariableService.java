/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.service;

import java.util.List;
import java.util.Map;

import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.dataset.domain.DatasetVariable;

public interface PublishedDatasetVariableService extends PublishedDocumentService<DatasetVariable> {
  long getCountByStudyId(String studyId);

  Map<String, Long> getCountByStudyIds(List<String> studyIds);

  long getCountByVariableType(DatasetVariable.Type type);

}
