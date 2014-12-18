package org.obiba.mica.dataset.service;

import org.obiba.mica.dataset.domain.Dataset;


public interface DatasetIndexer<T extends Dataset> {
  void onDatasetUpdated(T dataset);

  void onDatasetPublished(T dataset);

  void onDatasetDeleted(T dataset);

  void indexAll(Iterable<T> datasets, Iterable<T> publishedDatasets);
}
