/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search;

import org.springframework.data.domain.Persistable;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Search engine index manager.
 */
public interface Indexer {

  String[] ANALYZED_FIELDS = {"acronym", "name"};
  String MAPPING_NAME = "Variable";
  String HMAPPING_NAME = "H" + MAPPING_NAME;

  String DRAFT_DATASET_INDEX = "dataset-draft";
  String PUBLISHED_DATASET_INDEX = "dataset-published";
  String DATASET_TYPE = "Dataset"; // legacy: should be StudyDataset
  String STUDY_DATASET_TYPE = "StudyDataset";
  String HARMO_DATASET_TYPE = "HarmonizationDataset";

  String[] DATASET_LOCALIZED_ANALYZED_FIELDS = {"acronym", "name", "description"};

  String DRAFT_VARIABLE_INDEX = "variable-draft";
  String PUBLISHED_HVARIABLE_INDEX = "hvariable-published";
  String PUBLISHED_VARIABLE_INDEX = "variable-published";
  String VARIABLE_TYPE = MAPPING_NAME;
  String HARMONIZED_VARIABLE_TYPE = HMAPPING_NAME;
  String[] VARIABLE_ANALYZED_FIELDS = {"name", "label"};
  String[] VARIABLE_LOCALIZED_ANALYZED_FIELDS = {"label", "description"};

  String DRAFT_STUDY_INDEX = "study-draft";
  String PUBLISHED_STUDY_INDEX = "study-published";
  String STUDY_TYPE = "Study";
  String HARMO_STUDY_TYPE = "HarmonizationStudy";
  String[] STUDY_LOCALIZED_ANALYZED_FIELDS = {"acronym", "name", "objectives"};
  String DEFAULT_SORT_FIELD = "name";

  String DRAFT_NETWORK_INDEX = "network-draft";
  String PUBLISHED_NETWORK_INDEX = "network-published";
  String NETWORK_TYPE = "Network";
  String[] NETWORK_LOCALIZED_ANALYZED_FIELDS = {"acronym", "name", "description"};

  String DRAFT_PROJECT_INDEX = "project-draft";
  String PUBLISHED_PROJECT_INDEX = "project-published";
  String PROJECT_TYPE = "Project";
  String[] PROJECT_LOCALIZED_ANALYZED_FIELDS = {"title", "summary"};

  String PERSON_INDEX = "person";
  String DRAFT_PERSON_INDEX = "person-draft";
  String PERSON_TYPE = "Person";

  String ATTACHMENT_DRAFT_INDEX = "file-draft";
  String ATTACHMENT_PUBLISHED_INDEX = "file-published";
  String ATTACHMENT_TYPE = "AttachmentState";

  String TAXONOMY_INDEX = "taxonomy";
  String VOCABULARY_INDEX = "vocabulary";
  String TERM_INDEX = "term";
  String TAXONOMY_TYPE = "Taxonomy";
  String TAXONOMY_VOCABULARY_TYPE = "Vocabulary";
  String TAXONOMY_TERM_TYPE = "Term";
  String[] TAXONOMY_LOCALIZED_ANALYZED_FIELDS = {"title", "description", "keywords"};

  void index(String indexName, Persistable<String> persistable);

  void index(String indexName, Persistable<String> persistable, Persistable<String> parent);

  void index(String indexName, Indexable indexable);

  void index(String indexName, Indexable indexable, Indexable parent);

  void reIndexAllIndexables(String indexName, Iterable<? extends Indexable> persistables);

  void reindexAll(String indexName, Iterable<? extends Persistable<String>> persistables);

  void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables);

  void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables, Persistable<String> parent);

  void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables);

  void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables, @Nullable String parentId);

  void delete(String indexName, Persistable<String> persistable);

  void delete(String indexName, Indexable indexable);

  void delete(String indexName, String[] types, Map.Entry<String, String> termQuery);

  void delete(String indexName, String type, Map.Entry<String, String> termQuery);

  /**
   * Check if there is any index with given name.
   *
   * @param indexName
   * @return
   */
  boolean hasIndex(String indexName);

  /**
   * Drop index with given name.
   *
   * @param indexName
   */
  void dropIndex(String indexName);

  IndexFieldMapping getIndexfieldMapping(String indexName, String type);

  interface IndexConfigurationListener {
    void onIndexCreated(SearchEngineService searchEngineService, String indexName);
  }
}
