/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.entity.rql;

import com.google.common.collect.Lists;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.study.domain.BaseStudy;

import java.util.List;

/**
 * Various references associated to a Mica field query.
 */
public class RQLFieldReferences {
  private final DatasetVariable variable;
  private final Dataset dataset;
  private final List<BaseStudyTable> studyTables;
  private final BaseStudy study;
  private final String micaVariablePath;

  RQLFieldReferences(String micaVariablePath, Dataset dataset, BaseStudyTable studyTable, BaseStudy study, DatasetVariable variable) {
    this(micaVariablePath, dataset, Lists.newArrayList(studyTable), study, variable);
  }

  RQLFieldReferences(String micaVariablePath, Dataset dataset, List<BaseStudyTable> studyTables, BaseStudy study, DatasetVariable variable) {
    this.dataset = dataset;
    this.studyTables = studyTables;
    this.study = study;
    this.variable = variable;
    this.micaVariablePath = micaVariablePath;
  }

  public DatasetVariable getVariable() {
    return variable;
  }

  public Dataset getDataset() {
    return dataset;
  }

  public BaseStudy getStudy() {
    return study;
  }

  public List<BaseStudyTable> getStudyTables() {
    return studyTables;
  }

  String getOpalVariablePath(BaseStudyTable studyTable) {
    return getOpalTablePath(studyTable) + ":" + variable.getName();
  }

  String getOpalVariablePath() {
    return getOpalTablePath(studyTables.get(0)) + ":" + variable.getName();
  }

  String getMicaVariablePath() {
    return micaVariablePath;
  }


  public boolean hasStudyTableName() {
    return studyTables.get(0).getName() != null;
  }

  public LocalizedString getStudyTableName() {
    return studyTables.get(0).getName();
  }

  //
  // Private methods
  //

  /**
   * Get the Opal table path from the study table.
   *
   * @param studyTable
   * @return
   */
  private String getOpalTablePath(BaseStudyTable studyTable) {
    return studyTable.getSourceURN().replace("urn:opal:", "");
  }
}
