/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
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
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.service.DraftStudyDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
class EsDraftStudyDatasetService extends AbstractEsDatasetService<StudyDataset> implements DraftStudyDatasetService {

  private static final Logger log = LoggerFactory.getLogger(EsDraftStudyDatasetService.class);

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Override
  protected StudyDataset processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return objectMapper.readValue(inputStream, StudyDataset.class);
  }

  @Override
  protected String getIndexName() {
    return DatasetIndexer.DRAFT_DATASET_INDEX;
  }

  @Override
  protected String getType() {
    return DatasetIndexer.DATASET_TYPE;
  }

  @Override
  protected QueryBuilder filterByStudy(String studyId) {
    return QueryBuilders.boolQuery() //
      .should(QueryBuilders.termQuery("studyTable.studyId", studyId))
      .should(QueryBuilders.termQuery("studyTables.studyId", studyId));
  }

  @Override
  protected QueryBuilder filterByAccess() {
    List<String> ids = studyDatasetService.findAllStates().stream().map(StudyDatasetState::getId)
      .filter(s -> subjectAclService.isPermitted("/draft/study-dataset", "VIEW", s)).collect(Collectors.toList());

    return ids.isEmpty()
      ? QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"))
      : QueryBuilders.idsQuery().addIds(ids.stream().toArray(String[]::new));
  }
}
