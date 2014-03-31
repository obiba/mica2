package org.obiba.mica.service;

import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.obiba.mica.domain.Address;
import org.obiba.mica.domain.Contact;
import org.obiba.mica.domain.DataCollectionEvent;
import org.obiba.mica.domain.NumberOfParticipants;
import org.obiba.mica.domain.Population;
import org.obiba.mica.domain.Study;
import org.obiba.mica.repository.StudyRepository;
import org.springframework.stereotype.Component;

import static org.obiba.mica.domain.LocalizedString.en;
import static org.obiba.mica.domain.NumberOfParticipants.TargetNumber;
import static org.obiba.mica.domain.Population.SelectionCriteria.Gender;
import static org.obiba.mica.domain.Study.StudyMethods;

@SuppressWarnings({ "MagicNumber", "OverlyLongMethod" })
@Component
public class StudyGenerator {

  @Inject
  private StudyRepository studyRepository;

  @PostConstruct
  public void init() {

    Study study = createStudy();

    //TODO test GridFS

    studyRepository.save(study);
  }

  private Study createStudy() {
    Study study = new Study();
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

    study.setMethods(new StudyMethods());
    study.getMethods().getDesigns().add("case_control");
    study.getMethods().getDesigns().add("clinical_trial");
    study.getMethods().getDesigns().add("other");
    study.getMethods().setOtherDesign(en("Cross-sectional prevalence study"));

    study.getMethods().getRecruitments().add("individuals");
    study.getMethods().getRecruitments().add("other");
    study.getMethods().setOtherRecruitments(en("Specific individuals"));
    study.getMethods().setFollowUpInfos(en("General Information on Follow Up (profile and frequency)"));
    study.getMethods().setInfos(en("Supplementary information about study design"));

    study.setNumberOfParticipants(new NumberOfParticipants());
    study.getNumberOfParticipants().setParticipant(new TargetNumber());
    study.getNumberOfParticipants().getParticipant().setNumber(50_000);
    study.getNumberOfParticipants().setSample(new TargetNumber());
    study.getNumberOfParticipants().getSample().setNumber(30_000);

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
    population.setSelectionCriteria(new Population.SelectionCriteria());
    population.getSelectionCriteria().setAgeMin(45);
    population.getSelectionCriteria().setAgeMax(85);
    population.getSelectionCriteria().setGender(Gender.women);
    population.getSelectionCriteria().getCountriesIso().add(Locale.CANADA.getISO3Country());
    population.getSelectionCriteria()
        .setOtherCriteria(en("<p>Language: Individuals who are able to respond in either French or English.</p>\n" +
            "<p>Exclusion criteria: The CLSA uses the same exclusion criteria as the Statistics Canada Canadian Community Health Survey – Healthy Aging. Excluded from the study are:</p>\n" +
            "<ul><li>Residents of the three territories</li>\n" +
            "<li>Full-time members of the Canadian Forces</li>\n" +
            "<li>Individuals living in long-term care institutions (i.e., those providing 24-hour nursing care). However, those living in households and transitional housing arrangements (e.g., seniors’ residences, in which only minimal care is provided) will be included. CLSA cohort participants who become institutionalized during the course of the study will continue to be followed either through personal or proxy interview.</li>\n" +
            "<li>Persons living on reserves and other Aboriginal settlements. However, individuals who are of First Nations descent who live outside reserves are included in the study.</li>\n" +
            "<li>Individuals with cognitive impairment at baseline</li>\n" +
            "</ul>"));

    population.setRecruitment(new Population.Recruitment());
    population.getRecruitment().getDataSources().add("questionnaires");
    population.getRecruitment().getDataSources().add("administratives_databases");
    population.getRecruitment().getDataSources().add("others");
    population.getRecruitment().setOtherSource(en("Other source of recruitment"));

    population.getRecruitment().getGeneralPopulationSources().add("selected_samples");

    population.getRecruitment().getSpecificPopulationSources().add("clinic_patients");
    population.getRecruitment().getSpecificPopulationSources().add("other");
    population.getRecruitment().setOtherSpecificPopulationSource(en("Other specific population"));

    population.getRecruitment().getStudies().add(en("Canadian Community Health Survey (CCHS) – Healthy Aging"));
    population.getRecruitment().getStudies().add(en("CARTaGENE"));

    population.getDataCollectionEvents().add(createEvent1());
    population.getDataCollectionEvents().add(createEvent2());

    return population;
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
