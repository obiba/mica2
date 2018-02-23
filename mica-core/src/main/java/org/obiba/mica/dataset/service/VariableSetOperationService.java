/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.service;

import org.obiba.mica.core.domain.ComposedSet;
import org.obiba.mica.core.service.SetOperationService;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;

@Service
@Validated
public class VariableSetOperationService extends SetOperationService {

  @Inject
  private PublishedDatasetVariableService publishedDatasetVariableService;

  @Override
  public String getType() {
    return DatasetVariable.MAPPING_NAME;
  }

  @Override
  public long countDocuments(ComposedSet composedSet) {
    return publishedDatasetVariableService.countVariables(composedSet.getQuery());
  }

}
