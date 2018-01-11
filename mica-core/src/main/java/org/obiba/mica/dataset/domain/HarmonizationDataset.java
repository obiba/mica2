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
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.HarmonizationStudyTable;
import org.obiba.mica.core.domain.OpalTable;
import org.obiba.mica.core.domain.StudyTable;

/**
 * Dataset that relies on Study Opal servers summaries.
 */
public class HarmonizationDataset extends Dataset {

  private static final long serialVersionUID = -658603952811380458L;

  /**
   * Linked Harmonization Table
   */
  private HarmonizationStudyTable harmonizationTable;

  /**
   * Tables that implement the harmonization.
   */
  private List<StudyTable> studyTables;

  private List<HarmonizationStudyTable> harmonizationTables;

  public HarmonizationStudyTable getHarmonizationTable() {
    return harmonizationTable;
  }

  public boolean hasHarmonizationTable() {
    return harmonizationTable != null;
  }

  @JsonIgnore
  public HarmonizationStudyTable getSafeHarmonizationTable() {
    if (!hasHarmonizationTable()) throw new IllegalArgumentException("Harmonization Dataset is missing a harmonization table");
    return harmonizationTable;
  }

  public void setHarmonizationTable(HarmonizationStudyTable harmonizationTable) {
    this.harmonizationTable = harmonizationTable;
  }

  @NotNull
  public List<StudyTable> getStudyTables() {
    return studyTables == null ? studyTables = new ArrayList<>() : studyTables;
  }

  public void addStudyTable(StudyTable studyTable) {
    getStudyTables().add(studyTable);
  }

  public void setStudyTables(List<StudyTable> studyTables) {
    this.studyTables = studyTables;
  }

  public List<HarmonizationStudyTable> getHarmonizationTables() {
    return harmonizationTables == null ? harmonizationTables = new ArrayList<>() : harmonizationTables;
  }

  public void addHarmonizationTable(HarmonizationStudyTable harmonizationTable) {
    getHarmonizationTables().add(harmonizationTable);
  }

  public void setHarmonizationTables(List<HarmonizationStudyTable> harmonizationTables) {
    this.harmonizationTables = harmonizationTables;
  }

  @Override
  public String pathPrefix() {
    return "harmonizationDatasets";
  }

  @Override
  public Map<String, Serializable> parts() {
    HarmonizationDataset self = this;

    return new HashMap<String, Serializable>() {
      {
        put(self.getClass().getSimpleName(), self);
      }
    };
  }

  @JsonIgnore
  public List<BaseStudyTable> getBaseStudyTables() {
    return Lists.newArrayList(Iterables.concat(getStudyTables(), getHarmonizationTables())).stream()//
      .sorted(Comparator.comparingInt(OpalTable::getWeight)).collect(Collectors.toList());
  }
}
