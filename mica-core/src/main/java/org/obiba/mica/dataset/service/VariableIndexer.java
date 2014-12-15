package org.obiba.mica.dataset.service;

import java.util.List;
import java.util.Map;

import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;


public interface VariableIndexer {
  void onDatasetUpdated(Iterable<DatasetVariable> variables);

  void onDatasetUpdated(Iterable<DatasetVariable> variables, Map<String, List<DatasetVariable>> harmonizedVariables);

  void onDatasetPublished(Dataset dataset, Iterable<DatasetVariable> variables);

  void onDatasetPublished(Dataset dataset, Iterable<DatasetVariable> variables,
    Map<String, List<DatasetVariable>> harmonizedVariables);

  void onDatasetDeleted(Dataset dataset);

  void indexAll(Iterable<DatasetVariable> variables);

  void indexAll(Map<String, List<DatasetVariable>> harmonizedVariables);
}
