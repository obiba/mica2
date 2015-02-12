/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatasetAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final Logger log = LoggerFactory.getLogger(DatasetAggregationMetaDataProvider.class);

  @Inject
  PublishedDatasetService publishedDatasetService;

  private Map<String, LocalizedString> cacheTitles;

  private Map<String, LocalizedString> cacheDescriptions;

  public void refresh() {
    List<Dataset> datasets = publishedDatasetService.findAll();
    cacheTitles = datasets.stream()
      .collect(Collectors.toMap(Dataset::getId, Dataset::getAcronym));
    cacheDescriptions = datasets.stream()
      .collect(Collectors.toMap(Dataset::getId, Dataset::getName));
  }

  public MetaData getTitle(String aggregation, String termKey, String locale) {
    return "datasetId".equals(aggregation) && cacheTitles.containsKey(termKey)
      ? MetaData.newBuilder().title(cacheTitles.get(termKey).get(locale))
      .description(cacheDescriptions.get(termKey).get(locale)).build()
      : null;
  }

}
