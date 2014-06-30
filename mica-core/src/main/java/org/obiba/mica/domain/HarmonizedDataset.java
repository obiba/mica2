/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Dataset that relies on Study Opal servers summaries.
 */
public class HarmonizedDataset extends Dataset {

  private static final long serialVersionUID = -658603952811380458L;

  /**
   * Tables that implement the harmonization.
   */
  private List<StudyTable> studyTables;

  /**
   * Table that holds the variables in the primary Opal.
   */
  private String table;

  public List<StudyTable> getStudyTables() {
    return studyTables == null ? studyTables = new ArrayList<>() : studyTables;
  }

  public void addStudyTable(StudyTable studyTable) {
    getStudyTables().add(studyTable);
  }

  public void setStudyTables(List<StudyTable> studyTables) {
    this.studyTables = studyTables;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }
}
