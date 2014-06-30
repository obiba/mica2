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

import java.io.Serializable;

import javax.validation.constraints.NotNull;

/**
 * Represents
 */
public class StudyTable implements Serializable {

  private static final long serialVersionUID = -2466526849186256653L;

  private String studyId;

  @NotNull
  private String project;

  @NotNull
  private String table;

  public String getStudyId() {
    return studyId;
  }

  public void setStudyId(String studyId) {
    this.studyId = studyId;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }
}
