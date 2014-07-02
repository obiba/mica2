/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.domain;

import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.domain.StudyTable;

/**
 * Dataset that relies on Study Opal server to extract variables and summaries.
 */
public class StudyDataset extends Dataset {

  private static final long serialVersionUID = -658603952811380458L;

  private StudyTable studyTable;

  public StudyTable getStudyTable() {
    return studyTable;
  }

  public boolean hasStudyTable() {
    return studyTable != null;
  }

  public void setStudyTable(StudyTable studyTable) {
    this.studyTable = studyTable;
  }
}
