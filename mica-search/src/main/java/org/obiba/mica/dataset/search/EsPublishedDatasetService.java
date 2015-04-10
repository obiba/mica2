/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.SearchHit;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.search.AbstractPublishedDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
class EsPublishedDatasetService extends AbstractPublishedDocumentService<Dataset>
    implements PublishedDatasetService {

  private static final Logger log = LoggerFactory.getLogger(EsPublishedDatasetService.class);

  @Inject
  private ObjectMapper objectMapper;

  @Override
  protected Dataset processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return (Dataset) objectMapper.readValue(inputStream, getClass((String) hit.getSource().get("className")));
  }

  @Override
  protected String getIndexName() {
    return DatasetIndexerImpl.PUBLISHED_DATASET_INDEX;
  }

  @Override
  protected String getType() {
    return DatasetIndexerImpl.DATASET_TYPE;
  }

  private Class getClass(String className) {
    return StudyDataset.class.getSimpleName().equals(className) ? StudyDataset.class : HarmonizationDataset.class;
  }

  @Override
  protected FilterBuilder filterByStudy(String studyId) {
    return FilterBuilders.boolFilter().should( //
        FilterBuilders.termFilter("studyTable.studyId", studyId), //
        FilterBuilders.termFilter("studyTables.studyId", studyId));
  }
}
