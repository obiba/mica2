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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.obiba.mica.core.domain.DefaultEntityBase;
import org.obiba.mica.dataset.search.AbstractEsStudyService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EsPublishedStudyService extends AbstractEsStudyService<BaseStudy> implements PublishedStudyService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private StudyService collectionStudyService;

  @Inject
  private HarmonizationStudyService harmonizationStudyService;

  @Override
  public long getCollectionStudyCount() {
    return getCount(QueryBuilders.termQuery("className", Study.class.getSimpleName()));
  }

  @Override
  public long getHarmonizationStudyCount() {
    return getCount(QueryBuilders.termQuery("className", HarmonizationStudy.class.getSimpleName()));
  }

  @Override
  protected BaseStudy processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return (BaseStudy) objectMapper.readValue(inputStream, getClass((String) hit.getSource().get("className")));
  }

  @Override
  protected String getIndexName() {
    return StudyIndexer.PUBLISHED_STUDY_INDEX;
  }

  @Override
  protected String getType() {
    return StudyIndexer.COLLECTION_STUDY_TYPE;
  }

  @Override
  protected QueryBuilder filterByAccess() {
    if (micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> ids = collectionStudyService.findPublishedStates().stream().map(DefaultEntityBase::getId)
      .filter(s -> subjectAclService.isAccessible("/collection-study", s)).collect(Collectors.toList());
    ids.addAll(harmonizationStudyService.findPublishedStates().stream().map(DefaultEntityBase::getId)
      .filter(s -> subjectAclService.isAccessible("/harmonization-study", s)).collect(Collectors.toList()));
    return ids.isEmpty()
      ? QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"))
      : QueryBuilders.idsQuery().ids(ids);
  }

  private Class getClass(String className) {
    return Study.class.getSimpleName().equals(className) ? Study.class : HarmonizationStudy.class;
  }
}
