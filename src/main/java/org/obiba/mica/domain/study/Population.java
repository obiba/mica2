package org.obiba.mica.domain.study;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.LocalizableString;

public class Population implements Serializable {

  private static final long serialVersionUID = 6559914069652243954L;

  @NotNull
  private LocalizableString name;

  private LocalizableString description;

  private Recruitment recruitment;

  private SelectionCriteria selectionCriteria;

  private NumberOfParticipants numberOfParticipants;

  private LocalizableString infos;

  private List<DataCollectionEvent> dataCollectionEvents;

  public LocalizableString getName() {
    return name;
  }

  public void setName(LocalizableString name) {
    this.name = name;
  }

  public LocalizableString getDescription() {
    return description;
  }

  public void setDescription(LocalizableString description) {
    this.description = description;
  }

  public Recruitment getRecruitment() {
    return recruitment;
  }

  public void setRecruitment(Recruitment recruitment) {
    this.recruitment = recruitment;
  }

  public SelectionCriteria getSelectionCriteria() {
    return selectionCriteria;
  }

  public void setSelectionCriteria(SelectionCriteria selectionCriteria) {
    this.selectionCriteria = selectionCriteria;
  }

  public NumberOfParticipants getNumberOfParticipants() {
    return numberOfParticipants;
  }

  public void setNumberOfParticipants(NumberOfParticipants numberOfParticipants) {
    this.numberOfParticipants = numberOfParticipants;
  }

  public LocalizableString getInfos() {
    return infos;
  }

  public void setInfos(LocalizableString infos) {
    this.infos = infos;
  }

  public List<DataCollectionEvent> getDataCollectionEvents() {
    return dataCollectionEvents == null ? (dataCollectionEvents = new ArrayList<>()) : dataCollectionEvents;
  }

  public void setDataCollectionEvents(List<DataCollectionEvent> dataCollectionEvents) {
    this.dataCollectionEvents = dataCollectionEvents;
  }

  public static class Recruitment implements Serializable {

    private static final long serialVersionUID = 7949265355598902080L;

    public enum SourcesOfRecruitment {
      general, exist_studies, specific_population, other
    }

    public enum GeneralPopulation {
      volunteer, selected_samples, random
    }

    public enum SpecificPopulation {
      clinic_patients, specific_association, other
    }

    private List<String> sources;

    private List<String> generalPopulationSources;

    private List<String> specificPopulationSources;

    private LocalizableString otherSpecificPopulationSource;

    private List<LocalizableString> studies;

    private LocalizableString otherSource;

    private LocalizableString infos;

    public List<String> getSources() {
      return sources == null ? (sources = new ArrayList<>()) : sources;
    }

    public void setSources(List<String> sources) {
      this.sources = sources;
    }

    public List<String> getGeneralPopulationSources() {
      return generalPopulationSources == null
          ? (generalPopulationSources = new ArrayList<>())
          : generalPopulationSources;
    }

    public void setGeneralPopulationSources(List<String> generalPopulationSources) {
      this.generalPopulationSources = generalPopulationSources;
    }

    public List<String> getSpecificPopulationSources() {
      return specificPopulationSources == null
          ? (specificPopulationSources = new ArrayList<>())
          : specificPopulationSources;
    }

    public void setSpecificPopulationSources(List<String> specificPopulationSources) {
      this.specificPopulationSources = specificPopulationSources;
    }

    public LocalizableString getOtherSpecificPopulationSource() {
      return otherSpecificPopulationSource;
    }

    public void setOtherSpecificPopulationSource(LocalizableString otherSpecificPopulationSource) {
      this.otherSpecificPopulationSource = otherSpecificPopulationSource;
    }

    public List<LocalizableString> getStudies() {
      return studies == null ? (studies = new ArrayList<>()) : studies;
    }

    public void setStudies(List<LocalizableString> studies) {
      this.studies = studies;
    }

    public LocalizableString getOtherSource() {
      return otherSource;
    }

    public void setOtherSource(LocalizableString otherSource) {
      this.otherSource = otherSource;
    }

    public LocalizableString getInfos() {
      return infos;
    }

    public void setInfos(LocalizableString infos) {
      this.infos = infos;
    }
  }

  public static class SelectionCriteria implements Serializable {

    private static final long serialVersionUID = 310808673856023033L;

    public enum Gender {
      men, women
    }

    private Gender gender;

    @Min(0)
    private Integer ageMin;

    @Min(0)
    private Integer ageMax;

    private List<String> countriesIso;

    private LocalizableString territory;

    private List<String> criteria;

    private List<LocalizableString> ethnicOrigin;

    private List<LocalizableString> healthStatus;

    private LocalizableString otherCriteria;

    private LocalizableString infos;

    public Gender getGender() {
      return gender;
    }

    public void setGender(Gender gender) {
      this.gender = gender;
    }

    public Integer getAgeMin() {
      return ageMin;
    }

    public void setAgeMin(Integer ageMin) {
      this.ageMin = ageMin;
    }

    public Integer getAgeMax() {
      return ageMax;
    }

    public void setAgeMax(Integer ageMax) {
      this.ageMax = ageMax;
    }

    public List<String> getCountriesIso() {
      return countriesIso == null ? (countriesIso = new ArrayList<>()) : countriesIso;
    }

    public void setCountriesIso(List<String> countriesIso) {
      this.countriesIso = countriesIso;
    }

    public LocalizableString getTerritory() {
      return territory;
    }

    public void setTerritory(LocalizableString territory) {
      this.territory = territory;
    }

    public List<String> getCriteria() {
      return criteria;
    }

    public void setCriteria(List<String> criteria) {
      this.criteria = criteria;
    }

    public List<LocalizableString> getEthnicOrigin() {
      return ethnicOrigin == null ? (ethnicOrigin = new ArrayList<>()) : ethnicOrigin;
    }

    public void setEthnicOrigin(List<LocalizableString> ethnicOrigin) {
      this.ethnicOrigin = ethnicOrigin;
    }

    public List<LocalizableString> getHealthStatus() {
      return healthStatus == null ? (healthStatus = new ArrayList<>()) : healthStatus;
    }

    public void setHealthStatus(List<LocalizableString> healthStatus) {
      this.healthStatus = healthStatus;
    }

    public LocalizableString getOtherCriteria() {
      return otherCriteria;
    }

    public void setOtherCriteria(LocalizableString otherCriteria) {
      this.otherCriteria = otherCriteria;
    }

    public LocalizableString getInfos() {
      return infos;
    }

    public void setInfos(LocalizableString infos) {
      this.infos = infos;
    }
  }

}
