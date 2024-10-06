/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.DraftCollectedDatasetService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
class EsDraftCollectedDatasetService extends AbstractEsDatasetService<StudyDataset> implements DraftCollectedDatasetService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Override
  protected StudyDataset processHit(Searcher.DocumentResult res) throws IOException {
    return objectMapper.readValue(res.getSourceInputStream(), StudyDataset.class);
  }

  @Override
  protected String getIndexName() {
    return Indexer.DRAFT_DATASET_INDEX;
  }

  @Override
  protected String getType() {
    return Indexer.STUDY_DATASET_TYPE;
  }

  @Override
  protected String getStudyIdField() {
    return "studyTable.studyId";
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    return new Searcher.IdFilter() {
      @Override
      public Collection<String> getValues() {
        return collectedDatasetService.findAllIds().stream()
            .filter(s -> subjectAclService.isPermitted("/draft/collected-dataset", "VIEW", s))
            .collect(Collectors.toList());
      }
    };
  }
}
