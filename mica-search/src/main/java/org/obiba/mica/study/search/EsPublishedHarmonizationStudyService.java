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
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.HarmonizationStudyState;
import org.obiba.mica.study.service.AbstractStudyService;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@Service
public class EsPublishedHarmonizationStudyService extends EsPublishedStudyService<HarmonizationStudyState, HarmonizationStudy> {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private HarmonizationStudyService studyService;

  @Override
  public AbstractStudyService<HarmonizationStudyState, HarmonizationStudy> getStudyService() {
    return studyService;
  }

  @Override
  protected boolean isAccessible(String studyId) {
    return subjectAclService.isAccessible("/harmonization-study", studyId);
  }

  @Override
  protected HarmonizationStudy mapStreamToObject(InputStream inputStream) throws IOException {
    return objectMapper.readValue(inputStream, HarmonizationStudy.class);
  }

  @Override
  protected String getIndexName() {
    return StudyIndexer.HARMONIZATION_STUDY_TYPE;
  }

  @Override
  protected String getType() {
    return StudyIndexer.COLLECTION_STUDY_TYPE;
  }
}
