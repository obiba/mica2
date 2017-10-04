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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.obiba.mica.dataset.domain.*;
import org.obiba.mica.dataset.service.CollectionDatasetService;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
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
  private CollectionDatasetService collectionDatasetService;

  @Inject
  @Lazy
  private HarmonizationDatasetService harmonizationDatasetService;

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
    BoolQueryBuilder query = QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("className", HarmonizationDataset.class.getSimpleName()))
        .must(QueryBuilders.termQuery("harmonizationTable.studyId", studyId));

    return executeQuery(query, 0, MAX_SIZE)
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

  @Override
  protected QueryBuilder filterByAccess() {
    if (isOpenAccess()) return null;
    Collection<String> ids = getAccessibleIdFilter().getValues();
    return ids.isEmpty()
        ? QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"))
        : QueryBuilders.idsQuery().ids(ids);
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    if (isOpenAccess()) return null;
    return new Searcher.IdFilter() {
      @Override
      public Collection<String> getValues() {
        List<String> ids = collectionDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
            .filter(s -> subjectAclService.isAccessible("/collected-dataset", s)).collect(Collectors.toList());
        ids.addAll(harmonizationDatasetService.findPublishedStates().stream().map(HarmonizationDatasetState::getId)
            .filter(s -> subjectAclService.isAccessible("/harmonized-dataset", s)).collect(Collectors.toList()));
        return ids;
      }
    };
  }
}
