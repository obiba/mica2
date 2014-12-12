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

import org.apache.commons.lang.WordUtils;
import org.springframework.stereotype.Component;

@Component
public class DefaultAggregationMetaDataProvider implements AggregationMetaDataProvider {

  @Override
  public MetaData getTitle(String aggregation, String termKey, String locale) {
    return MetaData.newBuilder().title(WordUtils.capitalize(termKey.replaceAll("[-_]", " "))).description("").build();
  }

  @Override
  public void refresh() {
  }
}
