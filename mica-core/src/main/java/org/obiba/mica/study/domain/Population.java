package org.obiba.mica.study.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.bson.types.ObjectId;
import org.obiba.mica.domain.AbstractAttributeAware;
import org.obiba.mica.domain.AttributeAware;
import org.obiba.mica.domain.LocalizedString;

import com.google.common.collect.Iterables;

public class Population extends AbstractAttributeAware implements Serializable, Comparable<Population>, AttributeAware {

  private static final long serialVersionUID = 6559914069652243954L;

  private String id = new ObjectId().toString();

  @NotNull
  private LocalizedString name;

  private LocalizedString description;

  private Recruitment recruitment;

  private SelectionCriteria selectionCriteria;

  private NumberOfParticipants numberOfParticipants;

  private LocalizedString info;

  private SortedSet<DataCollectionEvent> dataCollectionEvents;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

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

  public LocalizedString getInfo() {
    return info;
  }

  public void setInfo(LocalizedString info) {
    this.info = info;
  }

  public SortedSet<DataCollectionEvent> getDataCollectionEvents() {
    return dataCollectionEvents;
  }

  public void addDataCollectionEvent(@NotNull DataCollectionEvent dataCollectionEvent) {
    if(dataCollectionEvents == null) dataCollectionEvents = new TreeSet<>();
    dataCollectionEvents.add(dataCollectionEvent);
  }

  public void setDataCollectionEvents(SortedSet<DataCollectionEvent> dataCollectionEvents) {
    this.dataCollectionEvents = dataCollectionEvents;
  }

  @Override
  public int hashCode() {return Objects.hash(id);}

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Population other = (Population) obj;
    return Objects.equals(id, other.id);
  }

  @Override
  public int compareTo(Population o) {
    if(dataCollectionEvents == null) return -1;
    if(o.dataCollectionEvents == null) return 1;
    int result = Iterables.get(dataCollectionEvents, 0).compareTo(Iterables.get(o.dataCollectionEvents, 0));
    return result == 0 ? id.compareTo(o.id) : result;
  }

  public static class Recruitment implements Serializable {

    private static final long serialVersionUID = 7949265355598902080L;

    private List<String> dataSources;

    private List<String> generalPopulationSources;

    private List<String> specificPopulationSources;

    private LocalizedString otherSpecificPopulationSource;

    private List<LocalizedString> studies;

    private LocalizedString otherSource;

    private LocalizedString info;

    public List<String> getDataSources() {
      return dataSources;
    }

    public void addDataSource(@NotNull String dataSource) {
      if(dataSources == null) dataSources = new ArrayList<>();
      dataSources.add(dataSource);
    }

    public void setDataSources(List<String> dataSources) {
      this.dataSources = dataSources;
    }

    public List<String> getGeneralPopulationSources() {
      return generalPopulationSources;
    }

    public void addGeneralPopulationSource(@NotNull String generalPopulationSource) {
      if(generalPopulationSources == null) generalPopulationSources = new ArrayList<>();
      generalPopulationSources.add(generalPopulationSource);
    }

    public void setGeneralPopulationSources(List<String> generalPopulationSources) {
      this.generalPopulationSources = generalPopulationSources;
    }

    public List<String> getSpecificPopulationSources() {
      return specificPopulationSources;
    }

    public void addSpecificPopulationSource(@NotNull String specificPopulationSource) {
      if(specificPopulationSources == null) specificPopulationSources = new ArrayList<>();
      specificPopulationSources.add(specificPopulationSource);
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
      return studies;
    }

    public void addStudy(@NotNull LocalizedString study) {
      if(studies == null) studies = new ArrayList<>();
      studies.add(study);
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

    public LocalizedString getInfo() {
      return info;
    }

    public void setInfo(LocalizedString info) {
      this.info = info;
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

    private LocalizedString info;

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
      return countriesIso;
    }

    public void addCountryIso(@NotNull String countryIso) {
      if(countriesIso == null) countriesIso = new ArrayList<>();
      countriesIso.add(countryIso);
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

    public void addCriteria(@NotNull String aCriteria) {
      if(criteria == null) criteria = new ArrayList<>();
      criteria.add(aCriteria);
    }

    public void setCriteria(List<String> criteria) {
      this.criteria = criteria;
    }

    public List<LocalizedString> getEthnicOrigin() {
      return ethnicOrigin;
    }

    public void addEthnicOrigin(@NotNull LocalizedString anEthnicOrigin) {
      if(ethnicOrigin == null) ethnicOrigin = new ArrayList<>();
      ethnicOrigin.add(anEthnicOrigin);
    }

    public void setEthnicOrigin(List<LocalizedString> ethnicOrigin) {
      this.ethnicOrigin = ethnicOrigin;
    }

    public List<LocalizedString> getHealthStatus() {
      return healthStatus;
    }

    public void addHealthStatus(@NotNull LocalizedString aHealthStatus) {
      if(healthStatus == null) healthStatus = new ArrayList<>();
      healthStatus.add(aHealthStatus);
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

    public LocalizedString getInfo() {
      return info;
    }

    public void setInfo(LocalizedString info) {
      this.info = info;
    }
  }

}
