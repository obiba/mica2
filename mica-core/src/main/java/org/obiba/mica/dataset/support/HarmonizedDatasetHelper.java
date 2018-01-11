/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.support;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.obiba.mica.core.domain.HarmonizationStudyTable;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.springframework.util.Assert;

import com.google.common.collect.Maps;

public final class HarmonizedDatasetHelper {


  public static TablesMerger newTablesMerger(List<HarmonizationDataset> datasets) {
    Assert.notNull(datasets, "Dataset list cannot be null.");
    return new TablesMerger(datasets);
  }

  public static class TablesMerger {
    private Map<String, StudyTable> sTableHashMap = Maps.newHashMap();
    private Map<String, HarmonizationStudyTable> hsTableHashMap = Maps.newHashMap();

    private TablesMerger(List<HarmonizationDataset> datasets) {
      datasets.forEach(dataset -> {
        dataset.getStudyTables().forEach(st -> sTableHashMap.put(st.getStudyId(), st));
        dataset.getHarmonizationTables().forEach(studyTable-> hsTableHashMap.put(studyTable.getStudyId(), studyTable));
      });
    }

    public Collection<StudyTable> getStudyTables() {
      return sTableHashMap.values();
    }

    public Collection<HarmonizationStudyTable> getHarmonizationStudyTables() {
      return hsTableHashMap.values();
    }

  }



}
