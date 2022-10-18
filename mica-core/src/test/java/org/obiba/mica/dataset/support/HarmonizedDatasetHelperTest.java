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

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.HarmonizationStudyTable;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import static org.obiba.mica.assertj.Assertions.assertThat;

import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class HarmonizedDatasetHelperTest {

  @Test
  public void test_empty_datasets() {
    List<HarmonizationDataset> datasets = Lists.newArrayList();
    HarmonizedDatasetHelper.TablesMerger tablesMerger = HarmonizedDatasetHelper.newTablesMerger(datasets);
    assertTrue(tablesMerger.getStudyTables().isEmpty());
    assertTrue(tablesMerger.getHarmonizationStudyTables().isEmpty());
  }

  @Test
  public void test_null_datasets() {
    try {
      HarmonizedDatasetHelper.newTablesMerger(null);
    } catch (IllegalArgumentException e) {
      assertTrue("Dataset list cannot be null.".equals(e.getMessage()));
    }
  }

  @Test
  public void test_only_distinct_tables() {
    List<HarmonizationDataset> datasets =
      Lists.newArrayList(
        createDataset(Lists.newArrayList("001", "002"), Lists.newArrayList("h001")),
        createDataset(Lists.newArrayList("003", "004"), Lists.newArrayList("h002"))
      );

    HarmonizedDatasetHelper.TablesMerger tablesMerger = HarmonizedDatasetHelper.newTablesMerger(datasets);
    assertThat(tablesMerger.getStudyTables().size()).isEqualTo(4);
    assertThat(tablesMerger.getHarmonizationStudyTables().size()).isEqualTo(2);
  }

  @Test
  public void test_tables_with_same_study_id() {
    List<HarmonizationDataset> datasets =
      Lists.newArrayList(
        createDataset(Lists.newArrayList("001", "002"), Lists.newArrayList("h001", "h002")),
        createDataset(Lists.newArrayList("002", "004"), Lists.newArrayList("h001", "h003"))
      );

    HarmonizedDatasetHelper.TablesMerger tablesMerger = HarmonizedDatasetHelper.newTablesMerger(datasets);
    assertThat(tablesMerger.getStudyTables().size()).isEqualTo(3);
    assertThat(tablesMerger.getHarmonizationStudyTables().size()).isEqualTo(3);
  }

  @Test
  public void test_tables_all_same_study_id() {
    List<HarmonizationDataset> datasets =
      Lists.newArrayList(
        createDataset(Lists.newArrayList("001", "002"), Lists.newArrayList("h001")),
        createDataset(Lists.newArrayList("001", "002"), Lists.newArrayList("h001"))
      );

    HarmonizedDatasetHelper.TablesMerger tablesMerger = HarmonizedDatasetHelper.newTablesMerger(datasets);
    assertThat(tablesMerger.getStudyTables().size()).isEqualTo(2);
    assertThat(tablesMerger.getHarmonizationStudyTables().size()).isEqualTo(1);
  }

  private HarmonizationDataset createDataset(List<String> studyIds, List<String> harmoStudyIds) {
    HarmonizationDataset dataset = new HarmonizationDataset();
    studyIds.forEach(id -> dataset.addStudyTable(createStudyTable(id)));
    harmoStudyIds.forEach(id -> dataset.addHarmonizationTable(createHarmonizationStudyTable(id)));

    return dataset;
  }

  private HarmonizationStudyTable createHarmonizationStudyTable(String studyId) {
    HarmonizationStudyTable table = new HarmonizationStudyTable();
    initTable(studyId, table);
    return table;
  }

  private StudyTable createStudyTable(String studyId) {
    StudyTable table = new StudyTable();
    initTable(studyId, table);
    table.setDataCollectionEventId(RandomStringUtils.random(10, true, true));
    return table;
  }

  private void initTable(String studyId, BaseStudyTable table) {
    table.setStudyId(studyId);
    table.setProject(RandomStringUtils.random(10, true, false));
    table.setTable(RandomStringUtils.random(10, true, false));
    table.setPopulationId(RandomStringUtils.random(10, true, true));
  }
}
