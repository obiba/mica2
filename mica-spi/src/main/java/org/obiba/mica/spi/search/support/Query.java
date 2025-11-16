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

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public interface Query {

  int DEFAULT_FROM = 0;

  int DEFAULT_SIZE = 10;

  /**
   * Get the from position when paging results.
   *
   * @return
   */
  default int getFrom() {
    return DEFAULT_FROM;
  }

  /**
   * Get the maximum count of results to be requested.
   *
   * @return
   */
  default int getSize() {
    return DEFAULT_SIZE;
  }

  /**
   * Get the fields names to be included in the source request.
   *
   * @return
   */
  List<String> getSourceFields();

  /**
   * @return List of aggregation buckets specified in the query.
   */
  @NotNull
  List<String> getQueryAggregationBuckets();

  /**
   * @return List of all aggregation buckets (additional and query specific) when executing the query.
   */
  @NotNull
  List<String> getAggregationBuckets();

  /**
   * This will ensure that {@link #getAggregationBuckets} includes the additional aggregation buckets.
   * @param additionalAggregationBuckets
   */
  void ensureAggregationBuckets(List<String> additionalAggregationBuckets);

  /**
   * @return List of aggregations specified in the query.
   */
  @NotNull
  List<String> getAggregations();

  /**
   * @return Returns a list of specific taxonomy terms to restrict the query aggregations when executing
   * coverage queries.
   */
  @NotNull
  Map<String, Map<String, List<String>>> getTaxonomyTermsMap();

  /**
   * Empty when there is no real query inside.
   *
   * @return
   */
  boolean isEmpty();

  /**
   * Empty when there is no real query inside.
   *
   * @return
   */
  boolean hasQueryBuilder();

  /**
   * When there is a statement about the id field.
   *
   * @return
   */
  boolean hasIdCriteria();
}
