package org.obiba.mica.web.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.obiba.git.command.GitCommandHandler;
import org.obiba.mica.domain.Address;
import org.obiba.mica.domain.Attachment;
import org.obiba.mica.domain.Authorization;
import org.obiba.mica.domain.Contact;
import org.obiba.mica.domain.DataCollectionEvent;
import org.obiba.mica.domain.MicaConfig;
import org.obiba.mica.domain.NumberOfParticipants;
import org.obiba.mica.domain.Population;
import org.obiba.mica.domain.Study;
import org.obiba.mica.domain.StudyState;
import org.obiba.mica.domain.Timestamped;
import org.obiba.mica.repository.MicaConfigRepository;
import org.obiba.mica.repository.StudyStateRepository;
import org.obiba.mica.service.GitService;
import org.obiba.mica.service.MicaConfigService;
import org.obiba.mica.service.StudyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import com.google.common.eventbus.EventBus;

import static org.mockito.Mockito.when;
import static org.obiba.mica.assertj.Assertions.assertThat;
import static org.obiba.mica.domain.LocalizedString.en;
import static org.obiba.mica.domain.Population.Recruitment;
import static org.obiba.mica.domain.Study.StudyMethods;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = StudyDtosTest.Config.class)
@DirtiesContext
@SuppressWarnings({ "MagicNumber", "OverlyCoupledClass" })
public class StudyDtosTest {

  @Inject
  private StudyService studyService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private Dtos dtos;

  @Test
  public void test_required_only_dto() throws Exception {
    Study study = new Study();
    study.setId("study_1");
    study.setName(en("Canadian Longitudinal Study on Aging"));
    study.setObjectives(en("The Canadian Longitudinal Study on Aging (CLSA) is a large, national, long-term study"));

    StudyState studyState = new StudyState();
    studyState.setId(study.getId());

    when(studyService.findStateByStudy(study)).thenReturn(studyState);
    when(micaConfigService.getConfig()).thenReturn(Mockito.mock(MicaConfig.class));
    when(micaConfigService.getConfig().getLocales()).thenReturn(Arrays.asList(Locale.ENGLISH, Locale.FRENCH));

    Mica.StudyDto dto = dtos.asDto(study);
    Study fromDto = dtos.fromDto(dto);
    assertTimestamps(studyState, dto);
    assertThat(fromDto).areFieldsEqualToEachOther(study);
  }

  @Test
  public void test_full_dto() throws Exception {
    Study study = createStudy();
    StudyState studyState = new StudyState();
    studyState.setId(study.getId());
    when(studyService.findStateByStudy(study)).thenReturn(studyState);
    when(micaConfigService.getConfig()).thenReturn(Mockito.mock(MicaConfig.class));
    when(micaConfigService.getConfig().getLocales()).thenReturn(Arrays.asList(Locale.ENGLISH, Locale.FRENCH));

    Mica.StudyDto dto = dtos.asDto(study);
    Study fromDto = dtos.fromDto(dto);
    assertTimestamps(studyState, dto);
    assertThat(fromDto).areFieldsEqualToEachOther(study);
  }

  private void assertTimestamps(Timestamped study, Mica.StudyDtoOrBuilder dto) {
    assertThat(dto.getTimestamps().getCreated()).isEqualTo(study.getCreatedDate().toString());
    assertThat(dto.getTimestamps().getLastUpdate())
        .isEqualTo(study.getLastModifiedDate() == null ? "" : study.getLastModifiedDate().toString());
  }

  private Study createStudy() {
    Study study = new Study();
    study.setId("study_1");
    study.setName(
        en("Canadian Longitudinal Study on Aging").forFr("Étude longitudinale canadienne sur le vieillissement"));
    study.setAcronym(en("CLSA"));
    study.setObjectives(
        en("The Canadian Longitudinal Study on Aging (CLSA) is a large, national, long-term study that will follow approximately 50,000 men and women between the ages of 45 and 85 for at least 20 years. The study will collect information on the changing biological, medical, psychological, social, lifestyle and economic aspects of people’s lives. These factors will be studied in order to understand how, individually and in combination, they have an impact in both maintaining health and in the development of disease and disability as people age.")
            .forFr(
                "L’Étude longitudinale canadienne sur le vieillissement (ÉLCV) est une vaste étude nationale à long terme qui permettra de suivre environ 50 000 Canadiennes et Canadiens âgé(e)s de 45 à 85 ans pendant une période d’au moins 20 ans. L’ÉLCV recueillera des renseignements sur les changements biologiques, médicaux, psychologiques, sociaux et sur les habitudes de vie qui se produisent chez les gens. On étudiera ces facteurs pour comprendre la façon dont ils influencent, individuellement et collectivement, le maintien en santé et le développement de maladies et d’incapacités au fur et à mesure que les gens vieillissent. L’ÉLCV sera l’une des études les plus complètes du genre entreprises jusqu’à ce jour, non seulement au Canada, mais aussi au niveau international.")
    );
    study.setWebsite("http://www.clsa-elcv.ca");

    Contact contact = createContact();
    study.addContact(contact);
    study.addInvestigator(contact);

    study.setStartYear(2002);

    study.setMethods(createMethods());
    study.setNumberOfParticipants(createNumberOfParticipants());

    study.addAccess("data");
    study.addAccess("bio_samples");
    study.addAccess("other");
    study.setOtherAccess(en("Other access"));

    study.setMarkerPaper(
        "Raina PS, Wolfson C, Kirkland SA, Griffith LE, Oremus M, Patterson C, Tuokko H, Penning M, Balion CM, Hogan D, Wister A, Payette H, Shannon H, and Brazil K, The Canadian longitudinal study on aging (CLSA). Can J Aging, 2009. 28(3): p. 221-9.");
    study.setPubmedId("PUBMED 19860977");

    study.addPopulation(createPopulation());
    study.setSpecificAuthorization(createAuthorization("opal"));
    study.setMaelstromAuthorization(createAuthorization("mica"));
    study.addAttachment(createAttachment());

    return study;
  }

  private Attachment createAttachment() {
    Attachment attachment = new Attachment();
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
    authorization.setDate(LocalDateTime.now());
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

  private StudyMethods createMethods() {
    StudyMethods methods = new StudyMethods();
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

  private Contact createContact() {
    Contact contact = new Contact();
    contact.setTitle("Dr.");
    contact.setFirstName("Parminder");
    contact.setLastName("Raina");
    contact.setEmail("praina@mcmaster.ca");
    contact.setPhone("1-905-525-9140 ext. 22197");

    Contact.Institution institution = new Contact.Institution();
    institution.setName(en("McMaster University"));
    institution.setDepartment(en("Department of Clinical Epidemiology & Biostatistics"));
    institution.setAddress(new Address());
    institution.getAddress().setCity(en("Hamilton"));
    institution.getAddress().setState("ON");
    institution.getAddress().setCountryIso(Locale.CANADA.getISO3Country());

    contact.setInstitution(institution);
    return contact;
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
    return population;
  }

  private Recruitment createRecruitment() {
    Recruitment recruitment = new Recruitment();
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
    criteria.setAgeMin(45);
    criteria.setAgeMax(85);
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
    event.setStartYear(2010);
    event.setEndYear(2015);
    event.addDataSource("questionnaires");
    event.addDataSource("physical_measures");
    event.addDataSource("biological_samples");
    event.addBioSample("BioSamples.blood");
    event.addBioSample("BioSamples.urine");
    event.addBioSample("BioSamples.others");
    event.setAdministrativeDatabases(Arrays.asList("aDB1"));
    event.setOtherBioSamples(en("Other biological sample"));
    return event;
  }

  private DataCollectionEvent createEvent2() {
    DataCollectionEvent event = new DataCollectionEvent();
    event.setName(en("Follow-Up One"));
    event.setDescription(en("First follow-up from baseline data collection"));
    event.setStartYear(2000);
    event.setStartMonth(1);
    event.setEndYear(2020);
    event.setEndYear(12);
    event.addDataSource("questionnaires");
    event.addDataSource("physical_measures");
    event.addDataSource("administratives_databases");
    event.addDataSource("others");
    event.setAdministrativeDatabases(Arrays.asList("aDB1", "aDB2"));
    event.setOtherDataSources(en("Other data sources"));
    event.setBioSamples(Arrays.asList("Blood", "Cell Tissue"));
    event.setTissueTypes(en("Liver Tissue"));
    event.setOtherBioSamples(en("Ear wax"));
    event.addAttachment(createAttachment());
    return event;
  }

  @Configuration
  static class Config {

    @Bean
    public Dtos dtos() {
      return new Dtos();
    }

    @Bean
    public StudyDtos studyDtos() {
      return new StudyDtos();
    }

    @Bean
    public ContactDtos contactDtos() {
      return new ContactDtos();
    }

    @Bean
    public StudyService studyService() {
      return Mockito.mock(StudyService.class);
    }

    @Bean
    public MicaConfigService micaConfigService() {
      return Mockito.mock(MicaConfigService.class);
    }

    @Bean
    public EventBus eventBus() {
      return Mockito.mock(EventBus.class);
    }

    @Bean
    public MicaConfigRepository micaConfigRepository() {
      return Mockito.mock(MicaConfigRepository.class);
    }

    @Bean
    public GitService gitService() {
      return Mockito.mock(GitService.class);
    }

    @Bean
    public GitCommandHandler gitCommandHandler() {
      return Mockito.mock(GitCommandHandler.class);
    }

    @Bean
    public StudyStateRepository studyStateRepository() {
      return Mockito.mock(StudyStateRepository.class);
    }

    @Bean
    public MicaConfigDtos micaConfigDtos() {
      return Mockito.mock(MicaConfigDtos.class);
    }

    @Bean
    public NetworkDtos networkDtos() {
      return Mockito.mock(NetworkDtos.class);
    }

  }
}
