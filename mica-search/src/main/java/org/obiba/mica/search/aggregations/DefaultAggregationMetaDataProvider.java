/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final Logger log = LoggerFactory.getLogger(DefaultAggregationMetaDataProvider.class);

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    return MetaData.newBuilder().title(termKey).description("").className("").build();
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    return true;
  }

  @Override
  public void refresh() {
  }
}
