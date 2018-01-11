/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.obiba.mica.core.domain.StudyTable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Dataset that relies on Study Opal server to extract variables and summaries.
 */
public class StudyDataset extends Dataset {

  private static final long serialVersionUID = -658603952811380458L;

  private StudyTable studyTable;

  public StudyTable getStudyTable() {
    return studyTable;
  }

  @JsonIgnore
  public StudyTable getSafeStudyTable() {
    if (!hasStudyTable()) throw new IllegalArgumentException("Dataset does not have a study table.");
    return studyTable;
  }

  public boolean hasStudyTable() {
    return studyTable != null;
  }

  public void setStudyTable(StudyTable studyTable) {
    this.studyTable = studyTable;
  }

  @Override
  public String pathPrefix() {
    return "studyDatasets";
  }

  @Override
  public Map<String, Serializable> parts() {
    StudyDataset self = this;

    return new HashMap<String, Serializable>() {
      {
        put(self.getClass().getSimpleName(), self);
      }
    };
  }
}
