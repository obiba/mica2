package org.obiba.mica.service;

import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.obiba.mica.domain.Address;
import org.obiba.mica.domain.Contact;
import org.obiba.mica.domain.study.NumberOfParticipants;
import org.obiba.mica.domain.study.Study;
import org.obiba.mica.repository.StudyRepository;
import org.springframework.stereotype.Component;

import static org.obiba.mica.domain.LocalizableString.en;
import static org.obiba.mica.domain.study.NumberOfParticipants.TargetNumber;
import static org.obiba.mica.domain.study.Study.StudyMethods;
import static org.obiba.mica.domain.study.Study.StudyMethods.Design;
import static org.obiba.mica.domain.study.Study.StudyMethods.RecruitmentTarget;

@SuppressWarnings({ "MagicNumber", "OverlyLongMethod" })
@Component
public class StudyGenerator {

  @Inject
  private StudyRepository studyRepository;

  @PostConstruct
  public void init() {
    createCLSA();
  }

  public void createCLSA() {

    Study study = new Study();
    study.setName(en("Canadian Longitudinal Study on Aging"));
    study.setAcronym(en("CLSA"));
    study.setObjectives(
        en("The Canadian Longitudinal Study on Aging (CLSA) is a large, national, long-term study that will follow approximately 50,000 men and women between the ages of 45 and 85 for at least 20 years. The study will collect information on the changing biological, medical, psychological, social, lifestyle and economic aspects of people’s lives. These factors will be studied in order to understand how, individually and in combination, they have an impact in both maintaining health and in the development of disease and disability as people age."));
    study.setWebsite("http://www.clsa-elcv.ca");

    Contact parminder = createContact();
    study.getContacts().add(parminder);
    study.getInvestigators().add(parminder);

    study.setStartYear(2002);

    study.setMethods(new StudyMethods());
    study.getMethods().getDesigns().add(Design.case_control.toString());
    study.getMethods().getDesigns().add(Design.clinical_trial.toString());
    study.getMethods().getDesigns().add(Design.other.toString());
    study.getMethods().setOtherDesign(en("Cross-sectional prevalence study"));

    study.getMethods().getRecruitments().add(RecruitmentTarget.individuals.toString());
    study.getMethods().getRecruitments().add(RecruitmentTarget.other.toString());
    study.getMethods().setOtherRecruitments(en("Specific individuals"));
    study.getMethods().setFollowUpInfos(en("General Information on Follow Up (profile and frequency)"));
    study.getMethods().setInfos(en("Supplementary information about study design"));

    study.setNumberOfParticipants(new NumberOfParticipants());
    study.getNumberOfParticipants().setParticipant(new TargetNumber());
    study.getNumberOfParticipants().getParticipant().setNumber(50_000);
    study.getNumberOfParticipants().setSample(new TargetNumber());
    study.getNumberOfParticipants().getSample().setNumber(30_000);

    studyRepository.save(study);

  }

  private Contact createContact() {
    Contact parminder = new Contact();
    parminder.setTitle("Dr.");
    parminder.setFirstName("Parminder");
    parminder.setLastName("Raina");
    parminder.setEmail("praina@mcmaster.ca");
    parminder.setPhone("1-905-525-9140 ext. 22197");

    Contact.Institution mcMaster = new Contact.Institution();
    mcMaster.setName(en("McMaster University"));
    mcMaster.setDepartment(en("Department of Clinical Epidemiology & Biostatistics"));
    mcMaster.setAddress(new Address());
    mcMaster.getAddress().setCity(en("Hamilton"));
    mcMaster.getAddress().setState("ON");
    mcMaster.getAddress().setCountryIso(Locale.CANADA.getISO3Country());

    parminder.setInstitution(mcMaster);
    return parminder;
  }

}
