/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.rql;

import com.google.common.base.Strings;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.study.domain.BaseStudy;

/**
 * Various references associated to a Mica field query.
 */
public class RQLFieldReferences {
  private final DatasetVariable variable;
  private final Dataset dataset;
  private final BaseStudyTable studyTable;
  private final BaseStudy study;
  private final String micaVariableReference;

  public RQLFieldReferences(String micaVariableReference, Dataset dataset, BaseStudyTable studyTable, BaseStudy study,
                            DatasetVariable variable) {
    this.dataset = dataset;
    this.studyTable = studyTable;
    this.study = study;
    this.variable = variable;
    this.micaVariableReference = micaVariableReference;
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

  public String getOpal() {
    return getOpal(study);
  }

  public String getTableReference() {
    return studyTable.getProject() + "." + studyTable.getTable();
  }

  public String getOpalVariableReference() {
    return getTableReference() + ":" + variable.getName();
  }

  public String getMicaVariableReference() {
    return micaVariableReference;
  }

  public static String getOpal(BaseStudy study) {
    return Strings.isNullOrEmpty(study.getOpal()) ? "_default" : study.getOpal();
  }
}
