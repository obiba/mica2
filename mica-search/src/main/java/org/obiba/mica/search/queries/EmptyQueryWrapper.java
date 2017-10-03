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

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.obiba.mica.spi.search.support.EmptyQuery;

import java.util.List;

/**
 *
 */
public class EmptyQueryWrapper extends EmptyQuery implements QueryWrapper {

  private QueryBuilder queryBuilder;

  @Override
  public boolean isValid() {
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
  public List<SortBuilder> getSortBuilders() {
    return null;
  }

}
