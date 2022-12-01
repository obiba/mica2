/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.source;

import org.obiba.magma.ValueTable;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;

/**
 * Describes the parameters to establish a connection with a Datasource
 * and retrieve a ValueTable containing the dataset's variables.
 */
public interface StudyTableSource {

  /**
   * Get the associated table name.
   *
   * @return
   */
  String getTable();

  /**
   * Get the {@link ValueTable} implementing the data dictionary and the data values.
   *
   * @return
   */
  ValueTable getValueTable();

  /**
   * Make a facet search, to retrieve contingency table results.
   *
   * @param query
   * @return
   */
  Search.QueryResultDto getFacets(Search.QueryTermsDto query);

  /**
   * Get a variable summary statistics.
   *
   * @param variableName
   * @return
   */
  Math.SummaryStatisticsDto getVariableSummary(String variableName);

  /**
   * URN representation of a value table source, indicates the identifier of the value table, in the namespace of the source.
   *
   * @return
   */
  String getURN();

}
