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
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.StudyService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@Service
public class EsPublishedCollectionStudyService extends EsPublishedStudyService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private StudyService studyService;

  @Override
  public StudyService getStudyService() {
    return studyService;
  }

  @Override
  protected boolean isAccessible(String studyId) {
    return subjectAclService.isAccessible("/study", studyId);
  }

  @Override
  protected Study mapStreamToObject(InputStream inputStream) throws IOException {
    return objectMapper.readValue(inputStream, Study.class);
  }

  @Override
  protected String getIndexName() {
    return StudyIndexer.PUBLISHED_STUDY_INDEX;
  }

  @Override
  protected String getType() {
    return StudyIndexer.COLLECTION_STUDY_TYPE;
  }
}
