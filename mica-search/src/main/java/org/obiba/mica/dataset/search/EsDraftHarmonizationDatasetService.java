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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.service.DraftHarmonizationDatasetService;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.search.AbstractDocumentService;
import org.obiba.mica.spi.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class EsDraftHarmonizationDatasetService extends AbstractDocumentService<HarmonizationDataset> implements DraftHarmonizationDatasetService {

  private static final Logger log = LoggerFactory.getLogger(EsDraftHarmonizationDatasetService.class);

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Override
  protected HarmonizationDataset processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return objectMapper.readValue(inputStream, HarmonizationDataset.class);
  }

  @Override
  protected String getIndexName() {
    return Indexer.DRAFT_DATASET_INDEX;
  }

  @Override
  protected String getType() {
    return Indexer.DATASET_TYPE;
  }

  @Override
  protected QueryBuilder filterByStudy(String studyId) {
    return QueryBuilders.boolQuery() //
      .should(QueryBuilders.termQuery("studyTable.studyId", studyId));
  }

  @Override
  protected QueryBuilder filterByAccess() {
    List<String> ids = harmonizationDatasetService.findAllStates().stream().map(HarmonizationDatasetState::getId)
      .filter(s -> subjectAclService.isPermitted("/draft/harmonized-dataset", "VIEW", s)).collect(Collectors.toList());

    return ids.isEmpty()
      ? QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"))
      : QueryBuilders.idsQuery().ids(ids);
  }
}
