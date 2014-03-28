package org.obiba.mica.web.model;

import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.obiba.mica.domain.Address;
import org.obiba.mica.domain.Contact;
import org.obiba.mica.domain.Timestamped;
import org.obiba.mica.domain.study.DataCollectionEvent;
import org.obiba.mica.domain.study.NumberOfParticipants;
import org.obiba.mica.domain.study.Population;
import org.obiba.mica.domain.study.Study;

import static org.obiba.mica.domain.LocalizedString.en;
import static org.obiba.mica.domain.study.Assertions.assertThat;
import static org.obiba.mica.domain.study.Study.StudyMethods;
import static org.obiba.mica.web.model.Mica.StudyDto;
import static org.obiba.mica.web.model.Mica.StudyDtoOrBuilder;

@SuppressWarnings("MagicNumber")
public class StudyDtosTest {

  private final Dtos dtos = new Dtos();

  @Test
  public void test_required_only_dto() throws Exception {
    Study study = new Study();
    study.setId("study_1");
    study.setName(en("Canadian Longitudinal Study on Aging"));
    study.setObjectives(en("The Canadian Longitudinal Study on Aging (CLSA) is a large, national, long-term study"));

    StudyDto dto = dtos.asDto(study);
    Study fromDto = dtos.fromDto(dto);
    assertTimestamps(study, dto);
    assertStudy(fromDto, study);
  }

  @Test
  public void test_full_dto() throws Exception {
    Study study = createStudy();
    StudyDto dto = dtos.asDto(study);
    Study fromDto = dtos.fromDto(dto);
    assertTimestamps(study, dto);
    assertStudy(fromDto, study);
  }

  private void assertTimestamps(Timestamped study, StudyDtoOrBuilder dto) {
    Assertions.assertThat(dto.getTimestamps().getCreated()).isEqualTo(study.getCreated().toString());
    Assertions.assertThat(dto.getTimestamps().getLastUpdate())
        .isEqualTo(study.getUpdated() == null ? "" : study.getUpdated().toString());
  }

  private void assertStudy(Study actual, Study expected) {
    assertThat(actual) //
        .isEqualTo(expected) //
        .hasName(expected.getName()) //
        .hasAcronym(expected.getAcronym()) //
            // TODO
//        .hasInvestigators(expected.getInvestigators()) //
//        .hasContacts(expected.getContacts()) //
        .hasObjectives(expected.getObjectives()) //
        .hasWebsite(expected.getWebsite());
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
    study.getContacts().add(contact);
    study.getInvestigators().add(contact);

    study.setStartYear(2002);

    study.setMethods(createMethods());
    study.setNumberOfParticipants(createNumberOfParticipants());

    study.getAccess().add("data");
    study.getAccess().add("bio_samples");
    study.getAccess().add("other");
    study.setOtherAccess(en("Other access"));

    study.setMarkerPaper(
        "Raina PS, Wolfson C, Kirkland SA, Griffith LE, Oremus M, Patterson C, Tuokko H, Penning M, Balion CM, Hogan D, Wister A, Payette H, Shannon H, and Brazil K, The Canadian longitudinal study on aging (CLSA). Can J Aging, 2009. 28(3): p. 221-9.");
    study.setPubmedId("PUBMED 19860977");

    study.getPopulations().add(createPopulation());
    return study;
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
    methods.getDesigns().add("case_control");
    methods.getDesigns().add("clinical_trial");
    methods.getDesigns().add("other");
    methods.setOtherDesign(en("Cross-sectional prevalence study"));

    methods.getRecruitments().add("individuals");
    methods.getRecruitments().add("other");
    methods.setOtherRecruitments(en("Specific individuals"));
    methods.setFollowUpInfos(en("General Information on Follow Up (profile and frequency)"));
    methods.setInfos(en("Supplementary information about study design"));
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
    population.setSelectionCriteria(getSelectionCriteria());
    population.setRecruitment(getRecruitment());
    population.getDataCollectionEvents().add(createEvent1());
    population.getDataCollectionEvents().add(createEvent2());
    return population;
  }

  private Population.Recruitment getRecruitment() {
    Population.Recruitment recruitment = new Population.Recruitment();
    recruitment.getDataSources().add("questionnaires");
    recruitment.getDataSources().add("administratives_databases");
    recruitment.getDataSources().add("others");
    recruitment.setOtherSource(en("Other source of recruitment"));

    recruitment.getGeneralPopulationSources().add("selected_samples");

    recruitment.getSpecificPopulationSources().add("clinic_patients");
    recruitment.getSpecificPopulationSources().add("other");
    recruitment.setOtherSpecificPopulationSource(en("Other specific population"));

    recruitment.getStudies().add(en("Canadian Community Health Survey (CCHS) – Healthy Aging"));
    recruitment.getStudies().add(en("CARTaGENE"));
    return recruitment;
  }

  private Population.SelectionCriteria getSelectionCriteria() {
    Population.SelectionCriteria criteria = new Population.SelectionCriteria();
    criteria.setAgeMin(45);
    criteria.setAgeMax(85);
    criteria.setGender(Population.SelectionCriteria.Gender.women);
    criteria.getCountriesIso().add(Locale.CANADA.getISO3Country());
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
    event.getDataSources().add("questionnaires");
    event.getDataSources().add("physical_measures");
    event.getDataSources().add("biological_samples");
    event.getBioSamples().add("BioSamples.blood");
    event.getBioSamples().add("BioSamples.urine");
    event.getBioSamples().add("BioSamples.others");
    event.setOtherBioSamples(en("Other biological sample"));
    return event;
  }

  private DataCollectionEvent createEvent2() {
    DataCollectionEvent event = new DataCollectionEvent();
    event.setName(en("Follow-Up One"));
    event.setDescription(en("First follow-up from baseline data collection"));
    event.setStartYear(2014);
    event.setEndYear(2018);
    event.getDataSources().add("questionnaires");
    event.getDataSources().add("physical_measures");
    event.getDataSources().add("administratives_databases");
    event.getDataSources().add("others");
    event.setOtherDataSources(en("Other data sources"));
    return event;
  }

}
