package org.obiba.mica.web.model;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.security.auth.callback.CallbackHandler;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.git.command.GitCommandHandler;
import org.obiba.mica.access.DataAccessRequestRepository;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.config.JsonConfiguration;
import org.obiba.mica.config.taxonomies.DatasetTaxonomy;
import org.obiba.mica.config.taxonomies.NetworkTaxonomy;
import org.obiba.mica.config.taxonomies.StudyTaxonomy;
import org.obiba.mica.config.taxonomies.VariableTaxonomy;
import org.obiba.mica.config.taxonomies.TaxonomyTaxonomy;
import org.obiba.mica.core.domain.Address;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.Authorization;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.domain.Timestamped;
import org.obiba.mica.core.notification.EntityPublicationFlowMailNotification;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.KeyStoreService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.TempFileRepository;
import org.obiba.mica.file.impl.GridFsService;
import org.obiba.mica.file.notification.FilePublicationFlowMailNotification;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.repository.MicaConfigRepository;
import org.obiba.mica.micaConfig.repository.OpalCredentialRepository;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.OpalCredentialService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.micaConfig.service.helper.OpalServiceHelper;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.network.NetworkStateRepository;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.project.ProjectRepository;
import org.obiba.mica.project.ProjectStateRepository;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.security.repository.SubjectAclRepository;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.StudyStateRepository;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.NumberOfParticipants;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.obiba.mica.assertj.Assertions.assertThat;
import static org.obiba.mica.core.domain.LocalizedString.en;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = { StudyDtosTest.Config.class, JsonConfiguration.class })
@DirtiesContext
@SuppressWarnings({ "MagicNumber", "OverlyCoupledClass" })
public class StudyDtosTest {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private Dtos dtos;

  @Inject
  private StudyService studyService;

  @Before
  public void before() {
    MicaConfig config = new MicaConfig();
    config.setLocales(Arrays.asList(Locale.ENGLISH, Locale.FRENCH));
    when(micaConfigService.getConfig()).thenReturn(config);
    when(studyService.getEntityState(anyString())).thenReturn(new StudyState());
  }

  @Test
  public void test_required_only_dto() throws Exception {
    Study study = new Study();
    study.setId(new ObjectId().toString());
    study.setName(en("Canadian Longitudinal Study on Aging"));
    study.setObjectives(en("The Canadian Longitudinal Study on Aging (CLSA) is a large, national, long-term study"));

    Mica.StudyDto dto = dtos.asDto(study, true);
    Study fromDto = dtos.fromDto(dto);
    assertTimestamps(study, dto);
    assertThat(fromDto).areFieldsEqualToEachOther(study);
  }

  private void assertTimestamps(Timestamped study, Mica.StudyDtoOrBuilder dto) {
    assertThat(dto.getTimestamps().getCreated()).isEqualTo(study.getCreatedDate().toString());
    assertThat(dto.getTimestamps().getLastUpdate())
      .isEqualTo(study.getLastModifiedDate() == null ? "" : study.getLastModifiedDate().toString());
  }

  @Test
  public void test_full_dto() throws Exception {
    Study study = createStudy();

    Mica.StudyDto dto = dtos.asDto(study, true);
    Study fromDto = dtos.fromDto(dto);
    assertTimestamps(study, dto);
    assertThat(fromDto).areFieldsEqualToEachOther(study);
  }

  @Test
  public void test_study_summary_dto() {
    Study study = createStudy();
    study.addPopulation(createPopulation());
    Mica.StudySummaryDto dto = dtos.asSummaryDto(study);
    assertThat(dto.getDataSources(0)).isEqualTo("questionnaires");
    assertThat(dto.getDataSourcesCount()).isEqualTo(5);
  }

  @SuppressWarnings("OverlyLongMethod")
  private Study createStudy() {
    Study study = new Study();
    study.setId("study_1");
    study.setName(
      en("Canadian Longitudinal Study on Aging").forFr("Étude longitudinale canadienne sur le vieillissement"));
    study.setAcronym(en("CLSA"));
    study.setObjectives(en(
      "The Canadian Longitudinal Study on Aging (CLSA) is a large, national, long-term study that will follow approximately 50,000 men and women between the ages of 45 and 85 for at least 20 years. The study will collect information on the changing biological, medical, psychological, social, lifestyle and economic aspects of people’s lives. These factors will be studied in order to understand how, individually and in combination, they have an impact in both maintaining health and in the development of disease and disability as people age.")
      .forFr(
        "L’Étude longitudinale canadienne sur le vieillissement (ÉLCV) est une vaste étude nationale à long terme qui permettra de suivre environ 50 000 Canadiennes et Canadiens âgé(e)s de 45 à 85 ans pendant une période d’au moins 20 ans. L’ÉLCV recueillera des renseignements sur les changements biologiques, médicaux, psychologiques, sociaux et sur les habitudes de vie qui se produisent chez les gens. On étudiera ces facteurs pour comprendre la façon dont ils influencent, individuellement et collectivement, le maintien en santé et le développement de maladies et d’incapacités au fur et à mesure que les gens vieillissent. L’ÉLCV sera l’une des études les plus complètes du genre entreprises jusqu’à ce jour, non seulement au Canada, mais aussi au niveau international."));
    study.setWebsite("http://www.clsa-elcv.ca");

    Person person = createPerson();
    study.setMemberships(new HashMap<String, List<Membership>>() {
      {
        put(Membership.CONTACT, Lists.newArrayList(new Membership(person, Membership.CONTACT)));
        put(Membership.INVESTIGATOR, Lists.newArrayList(new Membership(person, Membership.INVESTIGATOR)));
      }
    });

    study.setStart(2002);
    study.setEnd(2050);

    study.setMethods(createMethods());
    study.setNumberOfParticipants(createNumberOfParticipants());

    study.addAccess("data");
    study.addAccess("bio_samples");
    study.addAccess("other");
    study.setOtherAccess(en("Other access"));

    study
      .addAttribute(Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.FRENCH, "value fr").build());
    study
      .addAttribute(Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.ENGLISH, "value en").build());

    study.setMarkerPaper(
      "Raina PS, Wolfson C, Kirkland SA, Griffith LE, Oremus M, Patterson C, Tuokko H, Penning M, Balion CM, Hogan D, Wister A, Payette H, Shannon H, and Brazil K, The Canadian longitudinal study on aging (CLSA). Can J Aging, 2009. 28(3): p. 221-9.");
    study.setPubmedId("PUBMED 19860977");

    study.addPopulation(createPopulation());
    study.setSpecificAuthorization(createAuthorization("mica-server"));
    study.setMaelstromAuthorization(createAuthorization("mica"));
    study.setLogo(createAttachment());

    return study;
  }

  private Attachment createAttachment() {
    Attachment attachment = new Attachment();
    attachment.setId(new ObjectId().toString());
    attachment.setName("patate.frite");
    attachment.setType("zip");
    attachment.setDescription(en("This is an attachment"));
    attachment.setLang(Locale.ENGLISH);
    attachment.setSize(1_000_000);
    attachment.setMd5("7822fe77621b0b2c542215e599a3b511");
    return attachment;
  }

  private Authorization createAuthorization(String authorizer) {
    Authorization authorization = new Authorization();
    authorization.setAuthorizer(authorizer);
    authorization.setAuthorized(true);
    authorization.setDate(LocalDate.now());
    return authorization;
  }

  private NumberOfParticipants createNumberOfParticipants() {
    NumberOfParticipants numberOfParticipants = new NumberOfParticipants();
    numberOfParticipants.setParticipant(new NumberOfParticipants.TargetNumber());
    numberOfParticipants.getParticipant().setNumber(50_000);
    numberOfParticipants.setSample(new NumberOfParticipants.TargetNumber());
    numberOfParticipants.getSample().setNumber(30_000);
    return numberOfParticipants;
  }

  private Study.StudyMethods createMethods() {
    Study.StudyMethods methods = new Study.StudyMethods();
    methods.addDesign("case_control");
    methods.addDesign("clinical_trial");
    methods.addDesign("other");
    methods.setOtherDesign(en("Cross-sectional prevalence study"));

    methods.addRecruitment("individuals");
    methods.addRecruitment("other");
    methods.setOtherRecruitment(en("Specific individuals"));
    methods.setFollowUpInfo(en("General Information on Follow Up (profile and frequency)"));
    methods.setInfo(en("Supplementary information about study design"));
    return methods;
  }

  private Person createPerson() {
    Person person = new Person();
    person.setTitle("Dr.");
    person.setFirstName("Parminder");
    person.setLastName("Raina");
    person.setEmail("praina@mcmaster.ca");
    person.setPhone("1-905-525-9140 ext. 22197");

    Person.Institution institution = new Person.Institution();
    institution.setName(en("McMaster University"));
    institution.setDepartment(en("Department of Clinical Epidemiology & Biostatistics"));
    institution.setAddress(new Address());
    institution.getAddress().setCity(en("Hamilton"));
    institution.getAddress().setState("ON");
    institution.getAddress().setCountryIso(Locale.CANADA.getISO3Country());

    person.setInstitution(institution);
    return person;
  }

  private Population createPopulation() {
    Population population = new Population();
    population.setName(en("CLSA Population"));
    population.setDescription(en("This is a population"));
    population.setNumberOfParticipants(createNumberOfParticipants());
    population.setSelectionCriteria(createSelectionCriteria());
    population.setRecruitment(createRecruitment());
    population.addDataCollectionEvent(createEvent1());
    population.addDataCollectionEvent(createEvent2());
    population
      .addAttribute(Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.FRENCH, "value fr").build());
    population
      .addAttribute(Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.ENGLISH, "value en").build());
    return population;
  }

  private Population.Recruitment createRecruitment() {
    Population.Recruitment recruitment = new Population.Recruitment();
    recruitment.addDataSource("questionnaires");
    recruitment.addDataSource("administratives_databases");
    recruitment.addDataSource("others");
    recruitment.setOtherSource(en("Other source of recruitment"));
    recruitment.addGeneralPopulationSource("selected_samples");
    recruitment.addSpecificPopulationSource("clinic_patients");
    recruitment.addSpecificPopulationSource("other");
    recruitment.setOtherSpecificPopulationSource(en("Other specific population"));
    recruitment.addStudy(en("Canadian Community Health Survey (CCHS) – Healthy Aging")
      .forFr("Enquête sur la santé dans les collectivités canadiennes (ESCC) - Vieillissement en santé"));
    recruitment.addStudy(en("CARTaGENE"));
    return recruitment;
  }

  private Population.SelectionCriteria createSelectionCriteria() {
    Population.SelectionCriteria criteria = new Population.SelectionCriteria();
    criteria.setAgeMin(45.0);
    criteria.setAgeMax(85.0);
    criteria.setGender(Population.SelectionCriteria.Gender.women);
    criteria.addCountryIso(Locale.CANADA.getISO3Country());
    criteria.addCriteria("criteria1");
    criteria.addEthnicOrigin(en("Serbian"));
    criteria.addHealthStatus(en("Good"));
    criteria.setOtherCriteria(en("<p>Language: Individuals who are able to respond in either French or English.</p>\n" +
      "<p>Exclusion criteria: The CLSA uses the same exclusion criteria as the Statistics Canada Canadian Community Health Survey – Healthy Aging. Excluded from the study are:</p>\n" +
      "<ul><li>Residents of the three territories</li>\n" +
      "<li>Full-time members of the Canadian Forces</li>\n" +
      "<li>Individuals living in long-term care institutions (i.e., those providing 24-hour nursing care). However, those living in households and transitional housing arrangements (e.g., seniors’ residences, in which only minimal care is provided) will be included. CLSA cohort participants who become institutionalized during the course of the study will continue to be followed either through personal or proxy interview.</li>\n" +
      "<li>Persons living on reserves and other Aboriginal settlements. However, individuals who are of First Nations descent who live outside reserves are included in the study.</li>\n" +
      "<li>Individuals with cognitive impairment at baseline</li>\n" +
      "</ul>"));
    return criteria;
  }

  private DataCollectionEvent createEvent1() {
    DataCollectionEvent event = new DataCollectionEvent();
    event.setName(en("Baseline Recruitment"));
    event.setDescription(en("Baseline data collection"));
    event.setStart(2010, null);
    event.setEnd(2015, null);
    event.addDataSource("questionnaires");
    event.addDataSource("physical_measures");
    event.addDataSource("biological_samples");
    event.addBioSample("BioSamples.blood");
    event.addBioSample("BioSamples.urine");
    event.addBioSample("BioSamples.others");
    event.setAdministrativeDatabases(Arrays.asList("aDB1"));
    event.setOtherBioSamples(en("Other biological sample"));
    event
      .addAttribute(Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.FRENCH, "value fr").build());
    event
      .addAttribute(Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.ENGLISH, "value en").build());
    return event;
  }

  private DataCollectionEvent createEvent2() {
    DataCollectionEvent event = new DataCollectionEvent();
    event.setName(en("Follow-Up One"));
    event.setDescription(en("First follow-up from baseline data collection"));
    event.setStart(2000, null);
    event.setEnd(2020, null);
    event.addDataSource("questionnaires");
    event.addDataSource("physical_measures");
    event.addDataSource("administratives_databases");
    event.addDataSource("others");
    event.setAdministrativeDatabases(Arrays.asList("aDB1", "aDB2"));
    event.setOtherDataSources(en("Other data sources"));
    event.setBioSamples(Arrays.asList("Blood", "Cell Tissue"));
    event.setTissueTypes(en("Liver Tissue"));
    event.setOtherBioSamples(en("Ear wax"));
    return event;
  }

  @Configuration
  @ComponentScan("org.obiba.mica.web.model")
  static class Config {

    @Bean
    public NetworkTaxonomy networksConfiguration() {
      return mock(NetworkTaxonomy.class);
    }

    @Bean
    public StudyTaxonomy studiesConfiguration() {
      return mock(StudyTaxonomy.class);
    }

    @Bean
    public DatasetTaxonomy datasetsConfiguration() {
      return mock(DatasetTaxonomy.class);
    }

    @Bean
    public VariableTaxonomy variablesConfiguration() {
      return mock(VariableTaxonomy.class);
    }

    @Bean
    public TaxonomyTaxonomy taxonomyConfiguration() {
      return mock(TaxonomyTaxonomy.class);
    }

    @Bean
    public StudyService studyService() {
      return mock(StudyService.class);
    }

    @Bean
    public NetworkService networkService() {
      return mock(NetworkService.class);
    }

    @Bean
    public HarmonizationDatasetService harmonizationDatasetService() {
      return mock(HarmonizationDatasetService.class);
    }

    @Bean
    public OpalService opalService() {
      return mock(OpalService.class);
    }

    @Bean
    public OpalServiceHelper opalServiceHelper() {
      return mock(OpalServiceHelper.class);
    }

    @Bean
    public KeyStoreService keyStoreService() {
      return mock(KeyStoreService.class);
    }

    @Bean
    public OpalCredentialService opalCredentialService() {
      return mock(OpalCredentialService.class);
    }

    @Bean
    public OpalCredentialRepository opalCredentialRepository() {
      return mock(OpalCredentialRepository.class);
    }

    @Bean
    public CallbackHandler callbackHandler() {
      return mock(CallbackHandler.class);
    }

    @Bean
    public PublishedStudyService publishedStudyService() {
      return mock(PublishedStudyService.class);
    }

    @Bean
    public PublishedNetworkService publishedNetworkService() {
      return mock(PublishedNetworkService.class);
    }

    @Bean
    public MicaConfigService micaConfigService() {
      return mock(MicaConfigService.class);
    }

    @Bean
    public EventBus eventBus() {
      return mock(EventBus.class);
    }

    @Bean
    public MicaConfigRepository micaConfigRepository() {
      return mock(MicaConfigRepository.class);
    }

    @Bean
    public StudyDatasetRepository studyDatasetRepository() {
      return mock(StudyDatasetRepository.class);
    }

    @Bean
    public PersonRepository contactRepository() {
      return mock(PersonRepository.class);
    }

    @Bean
    public HarmonizationDatasetRepository harmonizationDatasetRepository() {
      return mock(HarmonizationDatasetRepository.class);
    }

    @Bean
    public MailService mailService() {
      return mock(MailService.class);
    }

    @Bean
    public NetworkRepository networkRepository() {
      return mock(NetworkRepository.class);
    }

    @Bean
    public NetworkStateRepository networkStateRepository() {
      return mock(NetworkStateRepository.class);
    }

    @Bean
    public GitService gitService() {
      return mock(GitService.class);
    }

    @Bean
    public TempFileService tempFileService() {
      return mock(TempFileService.class);
    }

    @Bean
    public TempFileRepository tempFileRepository() {
      return mock(TempFileRepository.class);
    }

    @Bean
    public GitCommandHandler gitCommandHandler() {
      return mock(GitCommandHandler.class);
    }

    @Bean
    public StudyStateRepository studyStateRepository() {
      return mock(StudyStateRepository.class);
    }

    @Bean
    public SubjectAclService subjectAclService() {
      return mock(SubjectAclService.class);
    }

    @Bean
    public SubjectAclRepository subjectAclRepository() {
      return mock(SubjectAclRepository.class);
    }

    @Bean
    public FileStoreService fsService() {
      return mock(GridFsService.class);
    }

    @Bean
    public GridFsOperations gridFsOperations() {
      return mock(GridFsOperations.class);
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
    public EntityPublicationFlowMailNotification entityPublicationFlowNotification() {
      return mock(EntityPublicationFlowMailNotification.class);
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
    public FilePublicationFlowMailNotification filePublicationFlowNotification() {
      return mock(FilePublicationFlowMailNotification.class);
    }
  }
}
