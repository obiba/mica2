/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.service;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.magma.Variable;
import org.obiba.magma.type.BooleanType;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.springframework.scheduling.annotation.AsyncResult;

import com.google.common.eventbus.EventBus;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class HarmonizedDatasetServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Spy
  @InjectMocks
  private HarmonizedDatasetService datasetService;

  @Mock
  private EventBus eventBus;

  @Mock
  private HarmonizedDatasetService.Helper helper;

  private StudyTable st;
  private StudyTable st2;

  private HarmonizationDataset dataset;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    st = buildStudyTable("proj", "table", "st");
    st2 = buildStudyTable("proj", "table", "st2");
    dataset = buildHarmonizationDataset("testds", st, st2);
  }

  @Test
  public void testPopulateHarmonizedVariablesMap() {
    List<DatasetVariable> l = new ArrayList<DatasetVariable>() {
      {
        add(new DatasetVariable(dataset, Variable.Builder.newVariable("v1", BooleanType.get(), "test").build(), st));
        add(new DatasetVariable(dataset, Variable.Builder.newVariable("v2", BooleanType.get(), "test").build(), st2));
      }};

    doReturn(dataset).when(datasetService).findById(anyString());
    when(helper.asyncGetDatasetVariables(any(Supplier.class))).thenReturn(new AsyncResult<>(l));
    doReturn(l).when(datasetService).getDatasetVariables(any(HarmonizationDataset.class));
    doReturn(l).when(datasetService).getDatasetVariables(any(HarmonizationDataset.class), any(StudyTable.class));

  }

  private HarmonizationDataset buildHarmonizationDataset(String id, StudyTable... studyTables) {
    HarmonizationDataset ds = new HarmonizationDataset();

    for(StudyTable s: studyTables) {
      ds.addStudyTable(s);
    }

    ds.setId(id);
    ds.setName(new LocalizedString(Locale.CANADA, "dataset" + id));

    return ds;
  }

  private StudyTable buildStudyTable(String project, String table, String studyId) {
    StudyTable st = new StudyTable();
    st.setProject(project);
    st.setTable(table);
    st.setStudyId(studyId);
    st.setPopulationId("pop");
    st.setDataCollectionEventId("ev");

    return st;
  }
}
