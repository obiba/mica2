/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
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

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.SearchHit;
import org.obiba.mica.search.AbstractPublishedDocumentService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EsPublishedStudyService extends AbstractPublishedDocumentService<Study> implements PublishedStudyService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private StudyService studyService;

  @Override
  public StudyService getStudyService() {
    return studyService;
  }

  @Override
  protected Study processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return objectMapper.readValue(inputStream, Study.class);
  }

  @Override
  protected String getIndexName() {
    return StudyIndexer.PUBLISHED_STUDY_INDEX;
  }

  @Override
  protected String getType() {
    return StudyIndexer.STUDY_TYPE;
  }

  @Override
  protected FilterBuilder filterByAccess() {
    if(micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> ids = studyService.findPublishedStates().stream().map(StudyState::getId)
      .filter(s -> subjectAclService.isAccessible("/study", s))
      .collect(Collectors.toList());
    return ids.isEmpty()
      ? FilterBuilders.notFilter(FilterBuilders.existsFilter("id"))
      : FilterBuilders.idsFilter().ids(ids.toArray(new String[ids.size()]));
  }

}
