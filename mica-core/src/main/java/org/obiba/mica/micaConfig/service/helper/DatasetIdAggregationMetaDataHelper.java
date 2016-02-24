/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service.helper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.micaConfig.service.helper.AbstractIdAggregationMetaDataHelper;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class DatasetIdAggregationMetaDataHelper extends AbstractIdAggregationMetaDataHelper {

  @Inject
  PublishedDatasetService publishedDatasetService;

  @Cacheable(value="aggregations-metadata", key = "'dataset'")
  public Map<String, AggregationMetaDataProvider.LocalizedMetaData> getDatasets() {
    List<Dataset> datasets= publishedDatasetService.findAll();
    return datasets.stream()
      .collect(
        Collectors.toMap(Dataset::getId, d -> new AggregationMetaDataProvider.LocalizedMetaData(d.getAcronym(), d.getName())));
  }

  @Override
  protected Map<String, AggregationMetaDataProvider.LocalizedMetaData> getIdAggregationMap() {
    return getDatasets();
  }
}
