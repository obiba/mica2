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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.obiba.magma.MagmaRuntimeException;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.OpalTableSource;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.opal.rest.client.magma.RestDatasource;
import com.google.common.eventbus.EventBus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CollectedDatasetServiceTest {

  @InjectMocks
  private CollectedDatasetService collectedDatasetService;

  @Mock
  private IndividualStudyService individualStudyService;

  @Mock
  private OpalService opalService;

  @Mock
  private StudyDatasetRepository studyDatasetRepository;

  @Mock
  private EventBus eventBus;

  @Mock
  private GitService gitService;

  @Mock
  private StudyDatasetStateRepository studyDatasetStateRepository;

  @Mock
  private MicaConfigService micaConfigService;

  private Study study;

  private StudyDataset dataset;

  private StudyDatasetState state;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    study = buildStudy();
    dataset = buildStudyDataset();
    state = buildStudyDatasetState(dataset);
    doNothing().when(gitService).save(any(StudyDataset.class), anyString());
    when(gitService.hasGitRepository(any(StudyDataset.class))).thenReturn(true);
    when(micaConfigService.getConfig()).thenReturn(new MicaConfig());
  }

  @Test
  public void testDatasourceConnectionErrorIsIgnoredForDraft() {
    RestDatasource r = mock(RestDatasource.class);
    when(r.getValueTable(anyString())).thenThrow(new MagmaRuntimeException());
    when(opalService.getDatasource(anyString(), anyString())).thenReturn(r);
    when(individualStudyService.findStudy(anyString())).thenReturn(study);
    when(studyDatasetStateRepository.findById(anyString()).get()).thenReturn(state);

    collectedDatasetService.save(dataset);
  }

  private StudyDataset buildStudyDataset() {
    StudyDataset ds = new StudyDataset();
    StudyTable st = new StudyTable();
    st.setSourceURN(OpalTableSource.newSource("proj", "tab").getURN());
    st.setPopulationId("1");
    st.setDataCollectionEventId("1");
    ds.setStudyTable(st);
    ds.setName(new LocalizedString(Locale.CANADA, "test"));

    return ds;
  }

  private StudyDatasetState buildStudyDatasetState(StudyDataset dataset) {
    StudyDatasetState state = new StudyDatasetState();
    state.setId(dataset.getId());

    return state;
  }

  private Study buildStudy() {
    Study s = new Study();
    s.setId("study");
    s.setOpal("opal");

    Population population = new Population();
    population.setId("1");
    DataCollectionEvent dce = new DataCollectionEvent();
    dce.setId("1");
    population.addDataCollectionEvent(dce);
    s.addPopulation(population);

    return s;
  }
}
