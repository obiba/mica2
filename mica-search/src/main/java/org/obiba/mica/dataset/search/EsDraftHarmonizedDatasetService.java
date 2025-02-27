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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.service.DraftHarmonizationDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.search.AbstractIdentifiedDocumentService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
class EsDraftHarmonizedDatasetService extends AbstractIdentifiedDocumentService<HarmonizationDataset> implements DraftHarmonizationDatasetService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Override
  protected HarmonizationDataset processHit(Searcher.DocumentResult res) throws IOException {
    return objectMapper.readValue(res.getSourceInputStream(), HarmonizationDataset.class);
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
  protected String getStudyIdField() {
    return "harmonizationTable.studyId";
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    return new Searcher.IdFilter() {
      @Override
      public Collection<String> getValues() {
        return harmonizedDatasetService.findAllIds().stream()
          .filter(s -> subjectAclService.isPermitted("/draft/harmonized-dataset", "VIEW", s))
          .collect(Collectors.toList());
      }
    };
  }
}
