/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.obiba.mica.search.AbstractIdentifiedDocumentService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.DraftIndividualStudyService;
import org.obiba.mica.study.service.IndividualStudyService;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EsDraftIndividualStudyService extends AbstractIdentifiedDocumentService<Study> implements DraftIndividualStudyService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private IndividualStudyService individualStudyService;

  @Override
  protected Study processHit(Searcher.DocumentResult res) throws IOException {
    return (Study) objectMapper.readValue(res.getSourceInputStream(), getClass(res.getClassName()));
  }

  @Override
  protected String getIndexName() {
    return Indexer.DRAFT_STUDY_INDEX;
  }

  @Override
  protected String getType() {
    return Indexer.STUDY_TYPE;
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    return new Searcher.IdFilter() {
      @Override
      public Collection<String> getValues() {
        List<String> ids = Lists.newArrayList();
        ids.addAll(findAuthorizedCollectionStudyIds());
        return ids;
      }
    };
  }

  private List<String> findAuthorizedCollectionStudyIds() {
    return individualStudyService.findAllIds().stream()
      .filter(studyId -> subjectAclService.isPermitted("/draft/individual-study", "VIEW", studyId))
      .collect(Collectors.toList());
  }

  private Class getClass(String className) {
    return Study.class.getSimpleName().equals(className) ? Study.class : HarmonizationStudy.class;
  }
}
