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

import org.obiba.mica.web.model.Mica;

/**
 * Helper class for implementing  {@link StudyTableSource}.
 */
public abstract class AbstractStudyTableSource implements StudyTableSource {

  private StudyTableContext context;

  @Override
  public void setStudyTableContext(StudyTableContext context) {
    this.context = context;
  }

  protected StudyTableContext getContext() {
    return context;
  }

  @Override
  public boolean providesContingency() {
    return false;
  }

  @Override
  public Mica.DatasetVariableContingencyDto getContingency(IVariable variable, IVariable crossVariable) {
    throw new UnsupportedOperationException("Contingency search not provided by: " + getClass().getSimpleName());
  }

  @Override
  public boolean providesVariableSummary() {
    return false;
  }

  @Override
  public Mica.DatasetVariableAggregationDto getVariableSummary(String variableName) {
    throw new UnsupportedOperationException("Summary statistics not provided by: " + getClass().getSimpleName());
  }
}
