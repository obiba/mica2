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
import java.util.Map;
import java.util.stream.Collectors;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.OpalTable;
import org.obiba.mica.core.service.DocumentSetService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Validated
public class VariableSetService extends DocumentSetService {

  private final PublishedDatasetVariableService publishedDatasetVariableService;

  private final PublishedDatasetService publishedDatasetService;

  @Inject
  public VariableSetService(
    PublishedDatasetVariableService publishedDatasetVariableService,
    PublishedDatasetService publishedDatasetService) {
    this.publishedDatasetVariableService = publishedDatasetVariableService;
    this.publishedDatasetService = publishedDatasetService;
  }

  @Override
  public String getType() {
    return DatasetVariable.MAPPING_NAME;
  }

  @Override
  public List<String> extractIdentifiers(String importedIdentifiers) {
    return extractIdentifiers(importedIdentifiers,
      id -> {
        DatasetVariable.Type type = DatasetVariable.IdResolver.from(id).getType();
        return DatasetVariable.Type.Collected.equals(type) || DatasetVariable.Type.Dataschema.equals(type);
      });
  }

  /**
   * Get variables from their identifiers.
   *
   * @param identifiers
   * @return
   */
  public List<DatasetVariable> getVariables(Set<String> identifiers) {
    return getVariables(identifiers, true);
  }

  /**
   * Get variables from their identifiers.
   *
   * @param identifiers
   * @param useCache
   * @return
   */
  public List<DatasetVariable> getVariables(Set<String> identifiers, boolean useCache) {
    return publishedDatasetVariableService.findByIds(Lists.newArrayList(identifiers), useCache);
  }

  /**
   * Get the variables referred by the {@link DocumentSet}.
   *
   * @param documentSet
   * @return
   */
  public List<DatasetVariable> getVariables(DocumentSet documentSet) {
    return getVariables(documentSet, true);
  }

  /**
   * Get the variables referred by the {@link DocumentSet}.
   *
   * @param documentSet
   * @param useCache
   * @return
   */
  public List<DatasetVariable> getVariables(DocumentSet documentSet, boolean useCache) {
    ensureType(documentSet);
    if (documentSet.getIdentifiers().isEmpty()) return Lists.newArrayList();
    return getVariables(documentSet.getIdentifiers(), useCache);
  }

  /**
   * Get a subset of the variables referred by the {@link DocumentSet}.
   *
   * @param documentSet
   * @param from
   * @param limit
   * @return
   */
  public List<DatasetVariable> getVariables(DocumentSet documentSet, int from, int limit) {
    ensureType(documentSet);
    if (documentSet.getIdentifiers().isEmpty()) return Lists.newArrayList();
    List<String> ids = Lists.newArrayList(documentSet.getIdentifiers());
    Collections.sort(ids);
    int to = from + limit;
    if (to > ids.size()) to = ids.size();
    return publishedDatasetVariableService.findByIds(ids.subList(from, to));
  }

  public List<String> toOpalVariableFullNames(String variableSetId) {
    DocumentSet documentSet = get(variableSetId);
    List<DatasetVariable> variables = publishedDatasetVariableService.findByIds(Lists.newArrayList(documentSet.getIdentifiers()));

    Map<String, String> opalTableFullNameMap = publishedDatasetService.findByIds(variables.stream()
      .map(DatasetVariable::getDatasetId)
      .distinct()
      .collect(Collectors.toList())
    ).stream()
      .collect(Collectors.toMap(Dataset::getId, this::toOpalTableFullName));

    return variables.stream()
      .filter(variable -> opalTableFullNameMap.containsKey(variable.getDatasetId()))
      .map(variable -> opalTableFullNameMap.get(variable.getDatasetId()) + ":" + variable.getName())
      .collect(Collectors.toList());
  }
  private String toOpalTableFullName(Dataset dataset) {
    OpalTable opalTable = dataset instanceof StudyDataset ? ((StudyDataset) dataset).getSafeStudyTable() : ((HarmonizationDataset) dataset).getSafeHarmonizationTable();
    return opalTable.getProject() + "." + opalTable.getTable();
  }
}
