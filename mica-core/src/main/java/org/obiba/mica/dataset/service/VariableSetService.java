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

import com.google.common.collect.Lists;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.service.DocumentSetService;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import java.util.List;

@Service
@Validated
public class VariableSetService extends DocumentSetService {

  @Inject
  private PublishedDatasetVariableService publishedDatasetVariableService;

  @Override
  public String getType() {
    return DatasetVariable.MAPPING_NAME;
  }

  @Override
  public List<String> extractIdentifiers(String importedIdentifiers) {
    return extractIdentifiers(importedIdentifiers,
      id -> DatasetVariable.Type.Collected.equals(DatasetVariable.IdResolver.from(id).getType()));
  }

  public List<DatasetVariable> getVariables(DocumentSet documentSet) {
    ensureType(documentSet);
    if (documentSet.getIdentifiers().isEmpty()) return Lists.newArrayList();
    return publishedDatasetVariableService.findByIds(Lists.newArrayList(documentSet.getIdentifiers()));
  }

  public List<DatasetVariable> getVariables(DocumentSet documentSet, int from, int limit) {
    ensureType(documentSet);
    if (documentSet.getIdentifiers().isEmpty()) return Lists.newArrayList();
    List<String> ids = Lists.newArrayList(documentSet.getIdentifiers());
    return publishedDatasetVariableService.findByIds(ids.subList(from, from + limit));
  }
}
