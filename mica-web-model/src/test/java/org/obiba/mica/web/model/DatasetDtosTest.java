/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.util.Arrays;
import java.util.Locale;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.obiba.mica.access.DataAccessRequestRepository;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.config.JsonConfiguration;
import org.obiba.mica.config.taxonomies.StudyTaxonomy;
import org.obiba.mica.core.ModelAwareTranslator;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.core.service.SchemaFormContentFileService;
import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.repository.DataAccessFormRepository;
import org.obiba.mica.micaConfig.repository.TaxonomyConfigRepository;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.TaxonomyConfigService;
import org.obiba.mica.project.ProjectRepository;
import org.obiba.mica.project.ProjectStateRepository;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.security.repository.SubjectAclRepository;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.HarmonizationStudyRepository;
import org.obiba.mica.study.HarmonizationStudyStateRepository;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.user.UserProfileService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.obiba.mica.core.domain.LocalizedString.en;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = { DatasetDtosTest.Config.class, JsonConfiguration.class })
@DirtiesContext
@SuppressWarnings({ "MagicNumber", "OverlyCoupledClass" })
public class DatasetDtosTest {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private StudyService studyService;

  @Inject
  private HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  @Inject
  private StudyDatasetStateRepository studyDatasetStateRepository;

  @Inject
  private Dtos dtos;

  @Before
  public void before() {
    MicaConfig config = new MicaConfig();
    config.setLocales(Arrays.asList(Locale.ENGLISH, Locale.FRENCH));
    when(micaConfigService.getConfig()).thenReturn(config);
  }

  // TODO complete this class by using StudyDtosTest class

  @Test
  public void test_study_dataset_dto() throws Exception {
    when(studyDatasetStateRepository.findOne(anyString())).thenReturn(new StudyDatasetState());
    StudyDataset studyDataset = createStudyDataset();
    Mica.DatasetDto dto = dtos.asDto(studyDataset);
    System.out.println(dto);
  }

  @Test
  public void test_harmonized_dataset_dto() throws Exception {
    when(harmonizationDatasetStateRepository.findOne(anyString())).thenReturn(new HarmonizationDatasetState());
    HarmonizationDataset harmonizationDataset = createHarmonizedDataset();
    Mica.DatasetDto dto = dtos.asDto(harmonizationDataset);
    System.out.println(dto);
  }

  private StudyDataset createStudyDataset() {
    StudyDataset studyDataset = new StudyDataset();
    studyDataset.setName(en("FNAC").forFr("FNAC"));
    studyDataset.setEntityType("Participant");
    StudyTable table = new StudyTable();
    table.setStudyId("1111111111111111");
    table.setPopulationId("1");
    table.setDataCollectionEventId("1");
    table.setProject("study1");
    table.setTable("FNAC");
    studyDataset.setStudyTable(table);
    studyDataset.addAttribute(Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.FRENCH, "value fr").value(
      Locale.ENGLISH, "Value en").build());
    studyDataset.addAttribute(
      Attribute.Builder.newAttribute("att2").namespace("mica").value(Locale.FRENCH, "value fr").build());

    StudyState state = new StudyState();
    state.setId("1111111111111111");
    when(studyService.getEntityState("1111111111111111")).thenReturn(state);

    return studyDataset;
  }

  private HarmonizationDataset createHarmonizedDataset() {
    HarmonizationDataset harmonizationDataset = new HarmonizationDataset();
    harmonizationDataset.setName(en("Healthy Obese Project").forFr("Projet des obeses en sante"));
    harmonizationDataset.setEntityType("Participant");
    harmonizationDataset.setProject("mica");
    harmonizationDataset.setTable("HOP");
    StudyTable table = new StudyTable();
    table.setStudyId("222222222222222");
    table.setPopulationId("2");
    table.setDataCollectionEventId("2");
    table.setProject("study1");
    table.setTable("HOP");
    harmonizationDataset.addStudyTable(table);

    StudyState state = new StudyState();
    state.setId("222222222222222");
    when(studyService.getEntityState("222222222222222")).thenReturn(state);

    return harmonizationDataset;
  }

  @Configuration
  @ComponentScan("org.obiba.mica.web.model")
  static class Config {

    @Bean
    public StudyTaxonomy studiesConfiguration() {
      return  mock(StudyTaxonomy.class);
    }

    @Bean
    public MicaConfigService micaConfigService() {
      return mock(MicaConfigService.class);
    }

    @Bean
    public PublishedDatasetVariableService datasetVariableService() {
      return mock(PublishedDatasetVariableService.class);
    }

    @Bean
    public PublishedStudyService publishedStudyService() {
      return Mockito.mock(PublishedStudyService.class);
    }

    @Bean
    public StudyRepository studyRepository() {
      return Mockito.mock(StudyRepository.class);
    }

    @Bean
    public HarmonizationStudyRepository harmonizationStudyRepository() {
      return Mockito.mock(HarmonizationStudyRepository.class);
    }

    @Bean
    public HarmonizationStudyStateRepository harmonizationStudyStateRepository() {
      return Mockito.mock(HarmonizationStudyStateRepository.class);
    }

    @Bean
    public StudyDatasetStateRepository studyDatasetStaetRepository() {
      return mock(StudyDatasetStateRepository.class);
    }

    @Bean
    public HarmonizationDatasetStateRepository harmonizationDatasetStaetRepository() {
      return mock(HarmonizationDatasetStateRepository.class);
    }

    @Bean
    public StudyService studyService() {
      return Mockito.mock(StudyService.class);
    }

    @Bean
    public HarmonizationStudyService harmonizationStudyService() {
      return Mockito.mock(HarmonizationStudyService.class);
    }

    @Bean
    public SubjectAclService subjectAclService() {
      return Mockito.mock(SubjectAclService.class);
    }

    @Bean
    public UserProfileService subjectProfileService() {
      return Mockito.mock(UserProfileService.class);
    }

    @Bean
    public SubjectAclRepository subjectAclRepository() {
      return Mockito.mock(SubjectAclRepository.class);
    }

    @Bean
    public DataAccessRequestUtilService dataAccessRequestUtilService() {
      return Mockito.mock(DataAccessRequestUtilService.class);
    }

    @Bean
    public SchemaFormContentFileService schemaFormContentFileService() {
      return Mockito.mock(SchemaFormContentFileService.class);
    }

    @Bean
    public DataAccessFormService dataAccessFormService() {
      return Mockito.mock(DataAccessFormService.class);
    }

    @Bean
    public DataAccessFormRepository dataAccessFormRepository() {
      return Mockito.mock(DataAccessFormRepository.class);
    }

    @Bean
    public FileSystemService fileSystemService() {
      return mock(FileSystemService.class);
    }

    @Bean
    public AttachmentRepository attachmentRepository() {
      return mock(AttachmentRepository.class);
    }

    @Bean
    public AttachmentStateRepository attachmentStateRepository() {
      return mock(AttachmentStateRepository.class);
    }

    @Bean
    public TaxonomyConfigService taxonomyConfigService() {
      return mock(TaxonomyConfigService.class);
    }

    @Bean
    public TaxonomyConfigRepository taxonomyConfigRepository() {
      return mock(TaxonomyConfigRepository.class);
    }

    @Bean
    public DataAccessRequestService dataAccessRequestService() {
      return mock(DataAccessRequestService.class);
    }

    @Bean
    public DataAccessRequestRepository dataAccessRequestRepository() {
      return mock(DataAccessRequestRepository.class);
    }

    @Bean
    public ProjectService projectService() {
      return mock(ProjectService.class);
    }

    @Bean
    public ProjectRepository projectRepository() {
      return mock(ProjectRepository.class);
    }

    @Bean
    public ProjectStateRepository projectStateRepository() {
      return mock(ProjectStateRepository.class);
    }

    @Bean
    public ModelAwareTranslator modelAwareTranslator() {
      return mock(ModelAwareTranslator.class);
    }
  }
}
