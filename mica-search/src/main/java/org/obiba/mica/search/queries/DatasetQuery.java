/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import java.util.Properties;

import javax.inject.Inject;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class DatasetQuery extends AbstractDocumentQuery {


  private static final String DATASET_FACETS_YML = "dataset-facets.yml";

  private static Properties joinFields = new Properties();

  static {
    joinFields.setProperty("studyTable.studyId", "");
    joinFields.setProperty("studyTables.studyId", "");
  }

  @Inject
  Dtos dtos;

  @Inject
  PublishedDatasetService publishedDatasetService;

  @Override
  public String getSearchIndex() {
    return DatasetIndexer.PUBLISHED_DATASET_INDEX;
  }

  @Override
  public String getSearchType() {
    return DatasetIndexer.DATASET_TYPE;
  }

  @Override
  public Resource getAggregationsDescription() {
    return new ClassPathResource(DATASET_FACETS_YML);
  }

  @Override
  public void processHits(MicaSearch.QueryResultDto.Builder builder, SearchHits hits) {
    MicaSearch.DatasetResultDto.Builder resBuilder = MicaSearch.DatasetResultDto.newBuilder();
    for(SearchHit hit : hits) {
      resBuilder.addDatasets(dtos.asDto(publishedDatasetService.findById(hit.getId())));
    }
    builder.setExtension(MicaSearch.DatasetResultDto.result, resBuilder.build());
  }

  @Override
  protected Properties getJoinFields() {
    return joinFields;
  }

}
