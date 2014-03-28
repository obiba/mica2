package org.obiba.mica.domain;

import java.io.Serializable;

public class Address implements Serializable {

  private static final long serialVersionUID = 8869937335553092873L;

  private LocalizedString street;

  private LocalizedString city;

  private String zip;

  private String state;

  private String countryIso;

  public LocalizedString getStreet() {
    return street;
  }

  public void setStreet(LocalizedString street) {
    this.street = street;
  }

  public LocalizedString getCity() {
    return city;
  }

  public void setCity(LocalizedString city) {
    this.city = city;
  }

  public String getZip() {
    return zip;
  }

  public void setZip(String zip) {
    this.zip = zip;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getCountryIso() {
    return countryIso;
  }

  public void setCountryIso(String countryIso) {
    this.countryIso = countryIso;
  }
}
