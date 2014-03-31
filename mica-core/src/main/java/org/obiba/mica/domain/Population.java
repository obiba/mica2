package org.obiba.mica.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class Population implements Serializable {

  private static final long serialVersionUID = 6559914069652243954L;

  @NotNull
  private LocalizedString name;

  private LocalizedString description;

  private Recruitment recruitment;

  private SelectionCriteria selectionCriteria;

  private NumberOfParticipants numberOfParticipants;

  private LocalizedString infos;

  private List<DataCollectionEvent> dataCollectionEvents;

  public LocalizedString getName() {
    return name;
  }

  public void setName(LocalizedString name) {
    this.name = name;
  }

  public LocalizedString getDescription() {
    return description;
  }

  public void setDescription(LocalizedString description) {
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

  public LocalizedString getInfos() {
    return infos;
  }

  public void setInfos(LocalizedString infos) {
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

    private List<String> dataSources;

    private List<String> generalPopulationSources;

    private List<String> specificPopulationSources;

    private LocalizedString otherSpecificPopulationSource;

    private List<LocalizedString> studies;

    private LocalizedString otherSource;

    private LocalizedString infos;

    public List<String> getDataSources() {
      return dataSources == null ? (dataSources = new ArrayList<>()) : dataSources;
    }

    public void setDataSources(List<String> dataSources) {
      this.dataSources = dataSources;
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

    public LocalizedString getOtherSpecificPopulationSource() {
      return otherSpecificPopulationSource;
    }

    public void setOtherSpecificPopulationSource(LocalizedString otherSpecificPopulationSource) {
      this.otherSpecificPopulationSource = otherSpecificPopulationSource;
    }

    public List<LocalizedString> getStudies() {
      return studies == null ? (studies = new ArrayList<>()) : studies;
    }

    public void setStudies(List<LocalizedString> studies) {
      this.studies = studies;
    }

    public LocalizedString getOtherSource() {
      return otherSource;
    }

    public void setOtherSource(LocalizedString otherSource) {
      this.otherSource = otherSource;
    }

    public LocalizedString getInfos() {
      return infos;
    }

    public void setInfos(LocalizedString infos) {
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

    private LocalizedString territory;

    private List<String> criteria;

    private List<LocalizedString> ethnicOrigin;

    private List<LocalizedString> healthStatus;

    private LocalizedString otherCriteria;

    private LocalizedString infos;

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

    public LocalizedString getTerritory() {
      return territory;
    }

    public void setTerritory(LocalizedString territory) {
      this.territory = territory;
    }

    public List<String> getCriteria() {
      return criteria;
    }

    public void setCriteria(List<String> criteria) {
      this.criteria = criteria;
    }

    public List<LocalizedString> getEthnicOrigin() {
      return ethnicOrigin == null ? (ethnicOrigin = new ArrayList<>()) : ethnicOrigin;
    }

    public void setEthnicOrigin(List<LocalizedString> ethnicOrigin) {
      this.ethnicOrigin = ethnicOrigin;
    }

    public List<LocalizedString> getHealthStatus() {
      return healthStatus == null ? (healthStatus = new ArrayList<>()) : healthStatus;
    }

    public void setHealthStatus(List<LocalizedString> healthStatus) {
      this.healthStatus = healthStatus;
    }

    public LocalizedString getOtherCriteria() {
      return otherCriteria;
    }

    public void setOtherCriteria(LocalizedString otherCriteria) {
      this.otherCriteria = otherCriteria;
    }

    public LocalizedString getInfos() {
      return infos;
    }

    public void setInfos(LocalizedString infos) {
      this.infos = infos;
    }
  }

}
