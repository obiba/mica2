/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search;

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
import org.obiba.mica.search.AbstractDocumentService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.DraftStudyService;
import org.obiba.mica.study.service.CollectionStudyService;
import org.springframework.stereotype.Service;

@Service
public class EsDraftStudyService extends AbstractDocumentService<Study> implements DraftStudyService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private CollectionStudyService collectionStudyService;

  @Override
  public CollectionStudyService getCollectionStudyService() {
    return collectionStudyService;
  }

  @Override
  protected Study processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return objectMapper.readValue(inputStream, Study.class);
  }

  @Override
  protected String getIndexName() {
    return StudyIndexer.DRAFT_STUDY_INDEX;
  }

  @Override
  protected String getType() {
    return StudyIndexer.STUDY_TYPE;
  }

  @Override
  protected QueryBuilder filterByAccess() {
    List<String> ids = collectionStudyService.findAllStates().stream().map(StudyState::getId)
      .filter(s -> subjectAclService.isPermitted("/draft/study", "VIEW", s))
      .collect(Collectors.toList());

    return ids.isEmpty()
      ? QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"))
      : QueryBuilders.idsQuery().ids(ids);
  }
}
