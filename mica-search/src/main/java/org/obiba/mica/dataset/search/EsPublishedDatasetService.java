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
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
class EsPublishedDatasetService extends AbstractEsDatasetService<Dataset> implements PublishedDatasetService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  @Lazy
  private CollectedDatasetService collectedDatasetService;

  @Inject
  @Lazy
  private HarmonizedDatasetService harmonizedDatasetService;

  @Override
  public long getStudyDatasetsCount() {
    return getCountByRql(String.format("in(className,%s)", StudyDataset.class.getSimpleName()));
  }

  @Override
  public long getHarmonizationDatasetsCount() {
    return getCountByRql(String.format("in(className,%s)", HarmonizationDataset.class.getSimpleName()));
  }

  @Override
  public List<HarmonizationDataset> getHarmonizationDatasetsByStudy(String studyId) {

    List<Dataset> datasets = executeRqlQuery(
      String.format("dataset(limit(0,%s),and(in(className,%s),in(harmonizationTable.studyId,%s)))",
        MAX_SIZE, HarmonizationDataset.class.getSimpleName(), studyId));

    return datasets
      .stream()
      .map(ds -> (HarmonizationDataset) ds).collect(Collectors.toList());
  }

  @Override
  protected Dataset processHit(Searcher.DocumentResult res) throws IOException {
    return (Dataset) objectMapper.readValue(res.getSourceInputStream(), getClass(res.getClassName()));
  }

  @Override
  protected String getIndexName() {
    return Indexer.PUBLISHED_DATASET_INDEX;
  }

  @Override
  protected String getType() {
    return Indexer.DATASET_TYPE;
  }

  private Class getClass(String className) {
    return StudyDataset.class.getSimpleName().equals(className) ? StudyDataset.class : HarmonizationDataset.class;
  }

  @Override
  protected String getStudyIdField() {
    return "studyTable.studyId";
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    if (isOpenAccess()) return null;
    return new Searcher.IdFilter() {
      @Override
      public Collection<String> getValues() {
        List<String> ids = collectedDatasetService.findPublishedIds().stream()
            .filter(s -> subjectAclService.isAccessible("/collected-dataset", s)).collect(Collectors.toList());
        ids.addAll(harmonizedDatasetService.findPublishedIds().stream()
            .filter(s -> subjectAclService.isAccessible("/harmonized-dataset", s)).collect(Collectors.toList()));
        return ids;
      }
    };
  }
}
