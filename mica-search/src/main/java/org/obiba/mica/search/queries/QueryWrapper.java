/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import java.util.List;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;

/**
 *
 */
public interface QueryWrapper {

  int DEFAULT_FROM = 0;

  int DEFAULT_SIZE = 10;

  boolean hasQueryBuilder();

  QueryBuilder getQueryBuilder();

  void setQueryBuilder(QueryBuilder queryBuilder);

  SortBuilder getSortBuilder();

  int getFrom();

  int getSize();

  List<String> getAggregationGroupBy();

  List<String> getAggregations();
}
