/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;

/**
 *
 */
public interface QueryWrapper {

  int DEFAULT_FROM = 0;

  int DEFAULT_SIZE = 10;

  boolean hasQueryBuilder();

  @Nullable
  QueryBuilder getQueryBuilder();

  void setQueryBuilder(QueryBuilder queryBuilder);

  @Nullable
  List<SortBuilder> getSortBuilders();

  int getFrom();

  int getSize();

  @NotNull
  List<String> getAggregationBuckets();

  @Nullable
  List<String> getAggregations();

  @NotNull
  Map<String, Map<String, List<String>>> getTaxonomyTermsMap();
}
