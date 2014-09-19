package org.obiba.mica;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Locale;

import javax.inject.Inject;

import org.obiba.mica.config.Profiles;
import org.obiba.mica.core.domain.Address;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.Authorization;
import org.obiba.mica.core.domain.Contact;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.file.TempFileService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.core.service.HarmonizationDatasetService;
import org.obiba.mica.core.service.StudyDatasetService;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.NumberOfParticipants;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.StudyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import static org.obiba.mica.core.domain.LocalizedString.en;
import static org.obiba.mica.study.domain.Study.StudyMethods;

@SuppressWarnings({ "MagicNumber", "OverlyLongMethod" })
@Component
@Profile(Profiles.DEV)
public class ApplicationSeed implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger log = LoggerFactory.getLogger(ApplicationSeed.class);

  @Inject
  private StudyService studyService;

  @Inject
  private TempFileService tempFileService;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Inject
  private NetworkService networkService;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    log.debug("Create new study - ContextStartedEvent");
    create();
  }

  public void create() {

    Network network = createNetwork();

    Study study = createStudy("CLSA", "ELCV", "Canadian Longitudinal Study on Aging",
        "Étude longitudinale canadienne sur le vieillissement");
    studyService.save(study);
    studyService.publish(study.getId());
    network.addStudy(study);

    StudyDataset studyDataset = new StudyDataset();
    studyDataset.setName(en("FNAC").forFr("FNAC"));
    studyDataset.setAcronym(en("FNAC"));
    studyDataset.setEntityType("Participant");
    StudyTable table = new StudyTable();
    table.setStudyId(study.getId());
    table.setProject("study1");
    table.setTable("FNAC");
    studyDataset.setStudyTable(table);
    studyDatasetService.save(studyDataset);
    studyDatasetService.publish(studyDataset.getId(), true);

    HarmonizationDataset harmonizationDataset = new HarmonizationDataset();
    harmonizationDataset.setName(en("Healthy Obese Project").forFr("Projet des obeses en sante"));
    harmonizationDataset.setAcronym(en("HOP"));
    harmonizationDataset.setDescription(
        en("The Healthy Obese Project (HOP) is a BioSHaRE-EU core project  which aims to evaluate the prevalence of the metabolically healthy obese, assess lifestyle determinants and clinical consequences of healthy obesity and explore genetic modifications and advanced metabolic profiling related to both the determinants and consequences of healthy obesity. The Healthy Obese Project (HOP) is separated into study phases that each assess a specific research question."));
    harmonizationDataset.setEntityType("Participant");
    harmonizationDataset.setProject("mica");
    harmonizationDataset.setTable("HOP");
    table = new StudyTable();
    table.setStudyId(study.getId());
    table.setProject("study1");
    table.setTable("HOP");
    harmonizationDataset.addStudyTable(table);
    harmonizationDatasetService.save(harmonizationDataset);
    harmonizationDatasetService.publish(harmonizationDataset.getId(), true);

    study = createStudy("NCDS", null, "National Child Development Study", "National Child Development Study");
    studyService.save(study);
    network.addStudy(study);

    networkService.save(network);
  }

  @SuppressWarnings("OverlyLongMethod")
  private Study createStudy(String acronymEn, String acronymFr, String nameEn, String nameFr) {
    Study study = new Study();
    study.setName(en(nameEn).forFr(nameFr));
    study.setAcronym(en(acronymEn).forFr(acronymFr));
    study.setObjectives(
        en("The Canadian Longitudinal Study on Aging (CLSA) is a large, national, long-term study that will follow approximately 50,000 men and women between the ages of 45 and 85 for at least 20 years. The study will collect information on the changing biological, medical, psychological, social, lifestyle and economic aspects of people’s lives. These factors will be studied in order to understand how, individually and in combination, they have an impact in both maintaining health and in the development of disease and disability as people age.")
            .forFr(
                "L’Étude longitudinale canadienne sur le vieillissement (ÉLCV) est une vaste étude nationale à long terme qui permettra de suivre environ 50 000 Canadiennes et Canadiens âgé(e)s de 45 à 85 ans pendant une période d’au moins 20 ans. L’ÉLCV recueillera des renseignements sur les changements biologiques, médicaux, psychologiques, sociaux et sur les habitudes de vie qui se produisent chez les gens. On étudiera ces facteurs pour comprendre la façon dont ils influencent, individuellement et collectivement, le maintien en santé et le développement de maladies et d’incapacités au fur et à mesure que les gens vieillissent. L’ÉLCV sera l’une des études les plus complètes du genre entreprises jusqu’à ce jour, non seulement au Canada, mais aussi au niveau international."));
    study.setWebsite("http://www.clsa-elcv.ca");
    study.setOpal("https://localhost:8443");

    Contact contact = createContact();
    study.addContact(contact);
    study.addInvestigator(contact);

    study.setStart(Year.of(2002));

    study.setMethods(createMethods());
    study.setNumberOfParticipants(createNumberOfParticipants());

    study.addAccess("data");
    study.addAccess("bio_samples");
    study.addAccess("other");
    study.setOtherAccess(en("Other access"));

    study.setMarkerPaper(
        "Raina PS, Wolfson C, Kirkland SA, Griffith LE, Oremus M, Patterson C, Tuokko H, Penning M, Balion CM, Hogan D, Wister A, Payette H, Shannon H, and Brazil K, The Canadian longitudinal study on aging (CLSA). Can J Aging, 2009. 28(3): p. 221-9.");
    study.setPubmedId("19860977");

    study.addPopulation(createPopulation());
    study.setSpecificAuthorization(createAuthorization("mica-server"));
    study.setMaelstromAuthorization(createAuthorization("mica"));
    study.addAttachment(createAttachment());

    study.addAttribute(
        Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.FRENCH, "value fr").build());
    study.addAttribute(
        Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.ENGLISH, "value en").build());

    return study;
  }

  private Attachment createAttachment() {
    try {
      TempFile tempFile = tempFileService.addTempFile("study-attachment.txt",
          new ByteArrayInputStream("This is an attachment".getBytes(StandardCharsets.UTF_8)));
      Attachment attachment = new Attachment();
      attachment.setId(tempFile.getId());
      attachment.setJustUploaded(true);
      attachment.setType("protocol");
      attachment.setDescription(en("This is an attachment"));
      attachment.setLang(Locale.ENGLISH);
      return attachment;
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
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
    population.addDataCollectionEvent(createEvent3());
    population.addAttribute(
        Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.FRENCH, "value fr").build());
    population.addAttribute(
        Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.ENGLISH, "value en").build());
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
    event.setStart(YearMonth.of(2010, 1));
    event.setEnd(YearMonth.of(2015, 12));
    event.addDataSource("questionnaires");
    event.addDataSource("physical_measures");
    event.addDataSource("biological_samples");
    event.addBioSample("BioSamples.blood");
    event.addBioSample("BioSamples.urine");
    event.addBioSample("BioSamples.others");
    event.setAdministrativeDatabases(Arrays.asList("aDB1"));
    event.setOtherBioSamples(en("Other biological sample"));
    event.addAttribute(
        Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.FRENCH, "value fr").build());
    event.addAttribute(
        Attribute.Builder.newAttribute("att1").namespace("mica").value(Locale.ENGLISH, "value en").build());
    return event;
  }

  private DataCollectionEvent createEvent2() {
    DataCollectionEvent event = new DataCollectionEvent();
    event.setName(en("Follow-Up One"));
    event.setDescription(en("First follow-up from baseline data collection"));
    event.setStart(YearMonth.of(2000, 1));
    event.setEnd(YearMonth.of(2020, 12));
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

  private DataCollectionEvent createEvent3() {
    DataCollectionEvent event = new DataCollectionEvent();
    event.setName(en("Pre-Selection"));
    event.setDescription(en("Pre-selection for baseline"));
    event.setStart(YearMonth.of(1996, 5));
    event.setEnd(YearMonth.of(2000, 11));
    event.addDataSource("questionnaires");
    event.addDataSource("physical_measures");
    event.addDataSource("administratives_databases");
    event.setAdministrativeDatabases(Arrays.asList("aDB1"));
    event.setOtherDataSources(en("Other data sources"));
    event.setBioSamples(Arrays.asList("Blood", "Cell Tissue"));
    event.addAttachment(createAttachment());
    return event;
  }

  private Network createNetwork() {
    Network network = new Network();
    network.setAcronym(en("BBMRI"));
    network.setName(en("Biobanking and Biomolecular Resources Research Infrastructure"));
    network.setPublished(true);

    Contact contact = createContact();
    network.addContact(contact);
    network.addInvestigator(contact);

    return network;
  }

}
