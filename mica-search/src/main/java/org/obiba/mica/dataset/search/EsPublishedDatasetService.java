/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;

import org.elasticsearch.search.SearchHits;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

@Service
class EsPublishedDatasetService extends AbstractPublishedDocumentService<Dataset>
    implements PublishedDatasetService {

  @Inject
  private ObjectMapper objectMapper;

  @Override
  protected List<Dataset> processHits(SearchHits hits) {
    List<Dataset> datasets = Lists.newArrayList();
    hits.forEach(hit -> {
      InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
      try {
        datasets
            .add((Dataset) objectMapper.readValue(inputStream, getClass((String) hit.getSource().get("className"))));
      } catch(IOException e) {
        throw new RuntimeException(e);
      }
    });

    return datasets;
  }

  @Override
  protected String getIndexName() {
    return DatasetIndexer.PUBLISHED_DATASET_INDEX;
  }

  @Override
  protected String getType() {
    return DatasetIndexer.DATASET_TYPE;
  }

  private Class getClass(String className) {
    return StudyDataset.class.getSimpleName().equals(className) ? StudyDataset.class : HarmonizationDataset.class;
  }
}
