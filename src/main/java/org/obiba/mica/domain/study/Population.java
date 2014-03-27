package org.obiba.mica.domain.study;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;

public class Population implements Serializable {

  private static final long serialVersionUID = 6559914069652243954L;

  @NotEmpty
  private String name;

  private String description;

  private Recruitment recruitment;

  private NumberOfParticipants numberOfParticipants;

  private SelectionCriteria selectionCriteria;

  public static class Recruitment implements Serializable {

    private static final long serialVersionUID = 7949265355598902080L;

    private List<String> sources;

    private List<String> generalPopulation;

    private List<String> specificPopulation;

    private List<String> studies;

    private String infos;

    public List<String> getSources() {
      return sources;
    }

    public void setSources(List<String> sources) {
      this.sources = sources;
    }

    public List<String> getGeneralPopulation() {
      return generalPopulation;
    }

    public void setGeneralPopulation(List<String> generalPopulation) {
      this.generalPopulation = generalPopulation;
    }

    public List<String> getSpecificPopulation() {
      return specificPopulation;
    }

    public void setSpecificPopulation(List<String> specificPopulation) {
      this.specificPopulation = specificPopulation;
    }

    public List<String> getStudies() {
      return studies;
    }

    public void setStudies(List<String> studies) {
      this.studies = studies;
    }

    public String getInfos() {
      return infos;
    }

    public void setInfos(String infos) {
      this.infos = infos;
    }
  }

  public static class SelectionCriteria implements Serializable {

    private static final long serialVersionUID = 310808673856023033L;

    public enum Gender {
      MEN, WOMEN
    }

    private Gender gender;

    @Min(0)
    private Integer ageMin;

    @Min(0)
    private Integer ageMax;

    private String country;

    private String territory;

    private List<String> criteria;

    private List<String> ethnicOrigin;

    private List<String> healthStatus;

    private String infos;

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

    public String getCountry() {
      return country;
    }

    public void setCountry(String country) {
      this.country = country;
    }

    public String getTerritory() {
      return territory;
    }

    public void setTerritory(String territory) {
      this.territory = territory;
    }

    public List<String> getCriteria() {
      return criteria;
    }

    public void setCriteria(List<String> criteria) {
      this.criteria = criteria;
    }

    public List<String> getEthnicOrigin() {
      return ethnicOrigin;
    }

    public void setEthnicOrigin(List<String> ethnicOrigin) {
      this.ethnicOrigin = ethnicOrigin;
    }

    public List<String> getHealthStatus() {
      return healthStatus;
    }

    public void setHealthStatus(List<String> healthStatus) {
      this.healthStatus = healthStatus;
    }

    public String getInfos() {
      return infos;
    }

    public void setInfos(String infos) {
      this.infos = infos;
    }
  }

}
