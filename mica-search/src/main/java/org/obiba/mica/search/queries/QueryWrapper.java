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

import javax.annotation.Nullable;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.obiba.mica.spi.search.support.Query;

/**
 *
 */
public interface QueryWrapper extends Query {


  @Nullable
  QueryBuilder getQueryBuilder();

  void setQueryBuilder(QueryBuilder queryBuilder);

  @Nullable
  List<SortBuilder> getSortBuilders();

}
