/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import org.obiba.mica.spi.search.CountStatsData;
import org.obiba.mica.spi.search.QueryMode;
import org.obiba.mica.spi.search.QueryScope;
import org.obiba.mica.spi.search.support.Query;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

public interface DocumentQueryInterface {

  Query getQuery();

  boolean isQueryNotEmpty();

  boolean hasQueryBuilder();

  void initialize(@Nullable Query query, String locale, QueryMode mode);

  QueryResultDto getResultQuery();

  /**
   * Verifies if the query has any criteria on the primary key (ID).
   *
   * @return true/false
   */
  boolean hasIdCriteria();

  /**
   * Executes query to extract study IDs from the aggregation results
   *
   * @return List of study IDs
   * @throws IOException
   */
  List<String> queryStudyIds();

  /**
   * Used on a document query to extract studsy IDs without details
   *
   * @param studyIds
   * @return
   * @throws IOException
   */
  List<String> queryStudyIds(List<String> studyIds);

  /**
   * Executes a filtered query to retrieve documents and aggregations.
   */
  void query(List<String> studyIds, CountStatsData counts, QueryScope scope);

  Map<String, Integer> getStudyCounts();
}
