package org.obiba.mica.dataset.service;

import org.obiba.mica.dataset.domain.Dataset;


public interface DatasetIndexer {
  void onDatasetUpdated(Dataset dataset);

  void onDatasetPublished(Dataset dataset);

  void onDatasetDeleted(Dataset dataset);

  void indexAll(Iterable<? extends Dataset> datasets, Iterable<? extends Dataset> publishedDatasets);

  void dropIndex();
}
