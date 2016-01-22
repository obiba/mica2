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
public class EmptyQueryWrapper implements QueryWrapper {

  private QueryBuilder queryBuilder;

  @Override
  public boolean hasQueryBuilder() {
    return queryBuilder != null;
  }

  @Override
  public QueryBuilder getQueryBuilder() {
    return queryBuilder;
  }

  @Override
  public void setQueryBuilder(QueryBuilder queryBuilder) {
    this.queryBuilder = queryBuilder;
  }

  @Override
  public SortBuilder getSortBuilder() {
    return null;
  }

  @Override
  public int getFrom() {
    return DEFAULT_FROM;
  }

  @Override
  public int getSize() {
    return DEFAULT_SIZE;
  }

  @Override
  public List<String> getAggregationGroupBy() {
    return null;
  }
}
