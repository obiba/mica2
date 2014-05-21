package org.obiba.mica.service.study;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.obiba.mica.domain.Address;
import org.obiba.mica.domain.Attachment;
import org.obiba.mica.domain.Authorization;
import org.obiba.mica.domain.Contact;
import org.obiba.mica.domain.DataCollectionEvent;
import org.obiba.mica.domain.Network;
import org.obiba.mica.domain.NumberOfParticipants;
import org.obiba.mica.domain.Population;
import org.obiba.mica.domain.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.obiba.mica.domain.LocalizedString.en;
import static org.obiba.mica.domain.Study.StudyMethods;

@SuppressWarnings({ "MagicNumber", "OverlyLongMethod" })
@Component
public class StudyGenerator {

  private static final Logger log = LoggerFactory.getLogger(StudyGenerator.class);

  @Inject
  private StudyService studyService;

//  @Inject
//  private NetworkRepository networkRepository;

  @PostConstruct
  public void init() {

    Study study = createStudy("CLSA", "Canadian Longitudinal Study on Aging", "Étude longitudinale canadienne sur le vieillissement");
    studyService.save(study);
    studyService.publish(study.getId());

    study = createStudy("NCDS", "National Child Development Study", "National Child Development Study");
    studyService.save(study);

//    Network network = createNetwork();
//    network.addStudy(study);
//    networkRepository.save(network);
//
//    studyRepository.findAll().forEach(s -> log.info(">> {}", s));
//    networkRepository.findAll().forEach(s -> log.info(">> {}", s));
  }

  @SuppressWarnings("OverlyLongMethod")
  private Study createStudy(String acronyme, String nameEn, String nameFr) {
    Study study = new Study();
    study.setName(
        en(nameEn).forFr(nameFr));
    study.setAcronym(en(acronyme));
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
    study.setPubmedId("19860977");

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
    population.addDataCollectionEvent(createEvent3());
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
    event.setEndMonth(12);
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
    event.setStartYear(1996);
    event.setStartMonth(5);
    event.setEndYear(2000);
    event.setEndMonth(11);
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
    network.setName(en("Biobanking and Biomolecular Resources Research Infrastructure"));
    return network;
  }

}
