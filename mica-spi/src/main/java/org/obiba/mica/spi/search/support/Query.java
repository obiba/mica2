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

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public interface Query {

  int DEFAULT_FROM = 0;

  int DEFAULT_SIZE = 10;

  default int getFrom() {
    return DEFAULT_FROM;
  }

  default int getSize() {
    return DEFAULT_SIZE;
  }

  List<String> getSourceFields();

  @NotNull
  List<String> getAggregationBuckets();

  @Nullable
  List<String> getAggregations();

  @NotNull
  Map<String, Map<String, List<String>>> getTaxonomyTermsMap();

  boolean isValid();
}
