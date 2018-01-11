/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service.helper;

import com.google.common.collect.Maps;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.obiba.mica.security.SubjectUtils.sudo;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DatasetIdAggregationMetaDataHelper extends AbstractIdAggregationMetaDataHelper {

  private static final Logger log = getLogger(DatasetIdAggregationMetaDataHelper.class);

  @Inject
  PublishedDatasetService publishedDatasetService;

  @Cacheable(value = "aggregations-metadata", key = "'dataset'")
  public Map<String, AggregationMetaDataProvider.LocalizedMetaData> getDatasets() {
    try {
      List<Dataset> datasets = sudo(() -> publishedDatasetService.findAll());
      return datasets.stream()
          .collect(
              Collectors.toMap(Dataset::getId, d -> new AggregationMetaDataProvider.LocalizedMetaData(d.getAcronym(), d.getName(), d.getClassName())));
    } catch (Exception e) {
      log.debug("Could not build Dataset aggregation metadata {}", e);
      return Maps.newHashMap();
    }
  }

  @Override
  protected Map<String, AggregationMetaDataProvider.LocalizedMetaData> getIdAggregationMap() {
    return getDatasets();
  }
}
