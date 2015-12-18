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
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.search.AbstractPublishedDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
class EsPublishedDatasetService extends AbstractPublishedDocumentService<Dataset> implements PublishedDatasetService {

  private static final Logger log = LoggerFactory.getLogger(EsPublishedDatasetService.class);

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Override
  protected Dataset processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return (Dataset) objectMapper.readValue(inputStream, getClass((String) hit.getSource().get("className")));
  }

  @Override
  protected String getIndexName() {
    return DatasetIndexer.PUBLISHED_DATASET_INDEX;
  }

  @Override
  protected String getType() {
    return DatasetIndexer.DATASET_TYPE;
  }

  private Class getClass(String className) {
    return StudyDataset.class.getSimpleName().equals(className) ? StudyDataset.class : HarmonizationDataset.class;
  }

  @Override
  protected QueryBuilder filterByStudy(String studyId) {
    return QueryBuilders.boolQuery() //
      .should(QueryBuilders.termQuery("studyTable.studyId", studyId))
      .should(QueryBuilders.termQuery("studyTables.studyId", studyId));
  }

  @Override
  protected QueryBuilder filterByAccess() {
    if(micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> ids = studyDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
      .filter(s -> subjectAclService.isAccessible("/study-dataset", s)).collect(Collectors.toList());
    ids.addAll(harmonizationDatasetService.findPublishedStates().stream().map(HarmonizationDatasetState::getId)
      .filter(s -> subjectAclService.isAccessible("/harmonization-dataset", s)).collect(Collectors.toList()));
    return ids.isEmpty()
      ? QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"))
      : QueryBuilders.idsQuery().ids(ids);
  }
}
