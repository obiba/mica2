/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search.support;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class EmptyQuery implements Query {

  private List<String> aggregationBuckets = Lists.newArrayList();

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public boolean hasQueryBuilder() {
    return false;
  }

  @Override
  public boolean hasIdCriteria() {
    return false;
  }

  @Override
  public List<String> getSourceFields() {
    return Lists.newArrayList();
  }

  @Override
  public List<String> getAggregationBuckets() {
    return aggregationBuckets;
  }

  @Override
  public List<String> getQueryAggregationBuckets() {
    return Lists.newArrayList();
  }

  @Override
  public void ensureAggregationBuckets(List<String> additionalAggregationBuckets) {
    for (String agg : additionalAggregationBuckets) {
      if (!aggregationBuckets.contains(agg)) aggregationBuckets.add(agg);
    }
  }

  @Override
  public List<String> getAggregations() {
    return Lists.newArrayList();
  }

  @Override
  public Map<String, Map<String, List<String>>> getTaxonomyTermsMap() {
    return Maps.newHashMap();
  }
}
