/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.tables;

import org.obiba.magma.ValueTable;
import org.obiba.mica.web.model.Mica;

/**
 * Describes the parameters to establish a connection with a Datasource
 * and retrieve a ValueTable containing the dataset's variables.
 */
public interface StudyTableSource {

  /**
   * Get the {@link ValueTable} implementing the data dictionary and the data values.
   *
   * @return
   */
  ValueTable getValueTable();

  /**
   * Whether crossing variables is supported.
   *
   * @return
   */
  boolean providesContingency();

  /**
   * Make a contingency query and return results.
   *
   * @param variable
   * @param crossVariable
   * @return
   */
  Mica.DatasetVariableContingencyDto getContingency(IVariable variable, IVariable crossVariable);

  /**
   * Whether variable summaries are supported.
   *
   * @return
   */
  boolean providesVariableSummary();

  /**
   * Get a variable summary statistics.
   *
   * @param variableName
   * @return
   */
  Mica.DatasetVariableAggregationDto getVariableSummary(String variableName);

  /**
   * URN representation of a value table source, indicates the identifier of the value table, in the namespace of the source.
   *
   * @return
   */
  String getURN();

  /**
   * Set context in which the study table is defined.
   *
   * @param context
   */
  void setStudyTableContext(StudyTableContext context);

}
