/*
 * Copyright (c) 2024 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.basic;


import org.obiba.mica.spi.search.support.EmptyQuery;
import org.obiba.mica.spi.search.support.JoinQuery;
import org.obiba.mica.spi.search.support.Query;

public class EmptyJoinQuery implements JoinQuery {
  @Override
  public boolean isWithFacets() {
    return false;
  }

  @Override
  public Query getVariableQuery() {
    return new EmptyQuery();
  }

  @Override
  public Query getDatasetQuery() {
    return new EmptyQuery();
  }

  @Override
  public Query getStudyQuery() {
    return new EmptyQuery();
  }

  @Override
  public Query getNetworkQuery() {
    return new EmptyQuery();
  }
}
