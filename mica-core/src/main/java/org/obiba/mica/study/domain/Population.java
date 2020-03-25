/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.domain;

import java.beans.Transient;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.obiba.mica.core.domain.AbstractAttributeModelAware;
import org.obiba.mica.core.domain.AttributeAware;
import org.obiba.mica.core.domain.LocalizedString;

public class Population extends AbstractAttributeModelAware implements Serializable, Comparable<Population>, AttributeAware {

  private static final long serialVersionUID = 6559914069652243954L;

  private String id;

  @NotNull
  private LocalizedString name;

  private LocalizedString description;

  private Recruitment recruitment;

  private SelectionCriteria selectionCriteria;

  private NumberOfParticipants numberOfParticipants;

  private LocalizedString info;

  private SortedSet<DataCollectionEvent> dataCollectionEvents = Sets.newTreeSet();

  private int weight;

  @JsonIgnore
  public boolean isNew() {
    return Strings.isNullOrEmpty(id);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    if(!Strings.isNullOrEmpty(id)) this.id = id;
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

  @Transient
  public List<DataCollectionEvent> getDataCollectionEventsSorted() {
    return dataCollectionEvents.stream().sorted(Comparator.comparing(DataCollectionEvent::getWeight))
      .collect(Collectors.toList());
  }

  public boolean hasDataCollectionEvents() {
    return dataCollectionEvents != null && !dataCollectionEvents.isEmpty();
  }

  public void addDataCollectionEvent(@NotNull DataCollectionEvent dataCollectionEvent) {
    if(dataCollectionEvents == null) dataCollectionEvents = new TreeSet<>();
    if(dataCollectionEvent.isNew()) {
      String newId = dataCollectionEvent.getName().asAcronym().asUrlSafeString().toLowerCase();
      if(hasDataCollectionEvent(newId)) {
        for(int i = 1; i < 1000; i++) {
          if(!hasDataCollectionEvent(newId + "_" + i)) {
            dataCollectionEvent.setId(newId + "_" + i);
            break;
          }
        }
      } else dataCollectionEvent.setId(newId);
    }
    dataCollectionEvents.add(dataCollectionEvent);
  }

  public boolean hasDataCollectionEvent(String dceId) {
    if(dataCollectionEvents == null) return false;
    for(DataCollectionEvent dce : dataCollectionEvents) {
      if(dce.getId().equals(dceId)) return true;
    }
    return false;
  }

  public DataCollectionEvent findDataCollectionEvent(String id) {
    return dataCollectionEvents.stream().filter(dce -> dce.getId().equals(id)).findFirst().orElse(null);
  }

  public List<String> getAllDataSources() {
    if(dataCollectionEvents != null) {
      return dataCollectionEvents.stream().filter(DataCollectionEvent::hasModel).flatMap(dce ->
        ((List<String>) dce.getModel().getOrDefault("dataSources", Lists.newArrayList())).stream()
      ).distinct().collect(Collectors.toList());
    }

    return null;
  }

  public void setDataCollectionEvents(SortedSet<DataCollectionEvent> dataCollectionEvents) {
    this.dataCollectionEvents = dataCollectionEvents == null ? Sets.newTreeSet() : dataCollectionEvents;
  }

  @Override
  public int hashCode() {return Objects.hash(id);}

  @Override
  @SuppressWarnings("SimplifiableIfStatement")
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null || getClass() != obj.getClass()) return false;
    return Objects.equals(id, ((Population) obj).id);
  }

  @Override
  public int compareTo(Population pop) {
    if(!hasDataCollectionEvents()) return 1;
    if(!pop.hasDataCollectionEvents()) return -1;
    int result = Iterables.get(dataCollectionEvents, 0).compareTo(Iterables.get(pop.dataCollectionEvents, 0));

    return result != 0 ? result : this.getId().compareTo(pop.getId());
  }

  @Override
  public Map<String, Object> getModel() {
    //TODO migration script

    if (!this.hasModel()) {
      Map<String, Object> map = Maps.newHashMap();

      if (this.getRecruitment() != null) map.put("recruitment", this.getRecruitment());
      if (this.getSelectionCriteria() != null) map.put("selectionCriteria", this.getSelectionCriteria());
      if (this.getNumberOfParticipants() != null) map.put("numberOfParticipants", this.getNumberOfParticipants());
      if (this.getInfo() != null) map.put("info", this.getInfo());

      this.setModel(map);
    }

    return super.getModel();
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int weight) {
    this.weight = weight;
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
    private Double ageMin;

    @Min(0)
    private Double ageMax;

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

    public Double getAgeMin() {
      return ageMin;
    }

    public void setAgeMin(Double ageMin) {
      this.ageMin = ageMin;
    }

    public Double getAgeMax() {
      return ageMax;
    }

    public void setAgeMax(Double ageMax) {
      this.ageMax = ageMax;
    }

    public List<String> getCountriesIso() {
      return countriesIso != null ? countriesIso : new ArrayList<>();
    }

    public void addCountryIso(@NotNull String countryIso) {
      getCountriesIso().add(countryIso);
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
