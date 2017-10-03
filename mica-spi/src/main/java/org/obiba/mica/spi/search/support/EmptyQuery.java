/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
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

  @Override
  public boolean isValid() {
    return false;
  }

  @Override
  public List<String> getSourceFields() {
    return Lists.newArrayList();
  }

  @Override
  public List<String> getAggregationBuckets() {
    return Lists.newArrayList();
  }

  @Override
  public List<String> getAggregations() {
    return null;
  }

  @Override
  public Map<String, Map<String, List<String>>> getTaxonomyTermsMap() {
    return Maps.newHashMap();
  }
}
