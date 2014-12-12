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

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.springframework.stereotype.Component;

@Component
public class DatasetAggregationMetaDataProvider implements AggregationMetaDataProvider {

  @Inject
  PublishedDatasetService publishedDatasetService;

  private Map<String, LocalizedString> cache;

  public MetaData getTitle(String aggregation, String termKey, String locale) {
    return aggregation.equals("datasetId")
      ? MetaData.newBuilder().title(cache.get(termKey).get(locale)).description("").build()
      : null;
  }

  @Override
  public void refresh() {
    cache = publishedDatasetService.findAll().stream().collect(Collectors.toMap(Dataset::getId, Dataset::getName));
  }

}
