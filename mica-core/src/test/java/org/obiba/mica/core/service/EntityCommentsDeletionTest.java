/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.network.NetworkStateRepository;
import org.obiba.mica.network.NoSuchNetworkException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.project.ProjectRepository;
import org.obiba.mica.project.ProjectStateRepository;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.service.NoSuchProjectException;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.study.ConstraintException;
import org.obiba.mica.study.HarmonizationStudyRepository;
import org.obiba.mica.study.HarmonizationStudyStateRepository;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.StudyStateRepository;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.IndividualStudyService;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Deleting a draft entity deletes its comments, stored by CommentsResource.createComment with the "/draft/<type>"
 * resource id and the entity id as instance id: otherwise a new entity reusing the id would show them.
 */
@ExtendWith(MockitoExtension.class)
public class EntityCommentsDeletionTest {

  private static final String ID = "abc";

  @InjectMocks
  private IndividualStudyService individualStudyService;

  @InjectMocks
  private HarmonizationStudyService harmonizationStudyService;

  @InjectMocks
  private NetworkService networkService;

  @InjectMocks
  private ProjectService projectService;

  @InjectMocks
  private CollectedDatasetService collectedDatasetService;

  // constructor injected, see init()
  private HarmonizedDatasetService harmonizedDatasetService;

  @Mock
  private CommentsService commentsService;

  @Mock
  private GitService gitService;

  @Mock
  private EventBus eventBus;

  @Mock
  private FileSystemService fileSystemService;

  @Mock
  private StudyRepository studyRepository;

  @Mock
  private StudyStateRepository studyStateRepository;

  @Mock
  private HarmonizationStudyRepository harmonizationStudyRepository;

  @Mock
  private HarmonizationStudyStateRepository harmonizationStudyStateRepository;

  @Mock
  private NetworkRepository networkRepository;

  @Mock
  private NetworkStateRepository networkStateRepository;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private ProjectStateRepository projectStateRepository;

  @Mock
  private StudyDatasetRepository studyDatasetRepository;

  @Mock
  private StudyDatasetStateRepository studyDatasetStateRepository;

  @Mock
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Mock
  private HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  @Mock
  private CollectedDatasetService.Helper collectedDatasetHelper;

  @BeforeEach
  public void init() {
    harmonizedDatasetService = new HarmonizedDatasetService(null, null, null, harmonizationDatasetRepository,
      harmonizationDatasetStateRepository, null, null, eventBus, fileSystemService, null, 1, 1);
    ReflectionTestUtils.setField(harmonizedDatasetService, "gitService", gitService);
    ReflectionTestUtils.setField(harmonizedDatasetService, "commentsService", commentsService);
  }

  @AfterEach
  public void shutdown() {
    harmonizedDatasetService.shutdownExecutor();
  }

  @Test
  public void test_delete_individual_study_deletes_its_comments() {
    Study study = withId(new Study());
    when(studyRepository.findById(ID)).thenReturn(Optional.of(study));

    individualStudyService.delete(ID);

    InOrder order = inOrder(studyRepository, commentsService, eventBus);
    order.verify(studyRepository).delete(study);
    order.verify(commentsService).delete("/draft/individual-study", ID);
    order.verify(eventBus).post(any());
  }

  @Test
  public void test_delete_harmonization_study_deletes_its_comments() {
    HarmonizationStudy study = withId(new HarmonizationStudy());
    when(harmonizationStudyRepository.findById(ID)).thenReturn(Optional.of(study));

    harmonizationStudyService.delete(ID);

    InOrder order = inOrder(harmonizationStudyRepository, commentsService, eventBus);
    order.verify(harmonizationStudyRepository).delete(study);
    order.verify(commentsService).delete("/draft/harmonization-study", ID);
    order.verify(eventBus).post(any());
  }

  @Test
  public void test_delete_network_deletes_its_comments() {
    Network network = withId(new Network());
    when(networkRepository.findById(ID)).thenReturn(Optional.of(network));

    networkService.delete(ID);

    InOrder order = inOrder(networkRepository, commentsService, eventBus);
    order.verify(networkRepository).delete(network);
    order.verify(commentsService).delete("/draft/network", ID);
    order.verify(eventBus).post(any());
  }

  @Test
  public void test_delete_project_deletes_its_comments() {
    when(projectRepository.findById(ID)).thenReturn(Optional.of(withId(new Project())));

    projectService.delete(ID);

    InOrder order = inOrder(projectRepository, commentsService, eventBus);
    order.verify(projectRepository).deleteById(ID);
    order.verify(commentsService).delete("/draft/project", ID);
    order.verify(eventBus).post(any());
  }

  @Test
  public void test_delete_collected_dataset_deletes_its_comments() {
    when(studyDatasetRepository.findById(ID)).thenReturn(Optional.of(withId(new StudyDataset())));

    collectedDatasetService.delete(ID);

    InOrder order = inOrder(studyDatasetRepository, commentsService, eventBus);
    order.verify(studyDatasetRepository).deleteById(ID);
    order.verify(commentsService).delete("/draft/collected-dataset", ID);
    order.verify(eventBus).post(any());
  }

  @Test
  public void test_delete_harmonized_dataset_deletes_its_comments() {
    when(harmonizationDatasetRepository.findById(ID)).thenReturn(Optional.of(withId(new HarmonizationDataset())));

    harmonizedDatasetService.delete(ID);

    InOrder order = inOrder(harmonizationDatasetRepository, commentsService, eventBus);
    order.verify(harmonizationDatasetRepository).deleteById(ID);
    order.verify(commentsService).delete("/draft/harmonized-dataset", ID);
    order.verify(eventBus).post(any());
  }

  //
  // Entity not deleted: its comments are kept
  //

  @Test
  public void test_delete_study_in_a_network_keeps_its_comments() {
    when(studyRepository.findById(ID)).thenReturn(Optional.of(withId(new Study())));
    when(networkRepository.findByStudyIds(ID)).thenReturn(Lists.newArrayList(withId(new Network())));

    assertThrows(ConstraintException.class, () -> individualStudyService.delete(ID));

    verifyNoInteractions(commentsService);
  }

  @Test
  public void test_delete_network_in_a_network_keeps_its_comments() {
    when(networkRepository.findById(ID)).thenReturn(Optional.of(withId(new Network())));
    Network parent = new Network();
    parent.setId("parent");
    when(networkRepository.findByNetworkIds(ID)).thenReturn(Lists.newArrayList(parent));

    assertThrows(ConstraintException.class, () -> networkService.delete(ID));

    verifyNoInteractions(commentsService);
  }

  @Test
  public void test_delete_unknown_entities_does_not_delete_comments() {
    assertThrows(NoSuchEntityException.class, () -> individualStudyService.delete(ID));
    assertThrows(NoSuchEntityException.class, () -> harmonizationStudyService.delete(ID));
    assertThrows(NoSuchNetworkException.class, () -> networkService.delete(ID));
    assertThrows(NoSuchProjectException.class, () -> projectService.delete(ID));
    assertThrows(NoSuchDatasetException.class, () -> collectedDatasetService.delete(ID));
    assertThrows(NoSuchDatasetException.class, () -> harmonizedDatasetService.delete(ID));

    verifyNoInteractions(commentsService);
  }

  private static <T extends AbstractGitPersistable> T withId(T entity) {
    entity.setId(ID);
    return entity;
  }
}
