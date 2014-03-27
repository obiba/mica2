package org.obiba.mica.domain;

import java.io.Serializable;

public class Address implements Serializable {

  private static final long serialVersionUID = 8869937335553092873L;

  private LocalizableString street;

  private LocalizableString city;

  private String zip;

  private LocalizableString state;

  private LocalizableString country;

  private String countryIso;

  public LocalizableString getStreet() {
    return street;
  }

  public void setStreet(LocalizableString street) {
    this.street = street;
  }

  public LocalizableString getCity() {
    return city;
  }

  public void setCity(LocalizableString city) {
    this.city = city;
  }

  public String getZip() {
    return zip;
  }

  public void setZip(String zip) {
    this.zip = zip;
  }

  public LocalizableString getState() {
    return state;
  }

  public void setState(LocalizableString state) {
    this.state = state;
  }

  public LocalizableString getCountry() {
    return country;
  }

  public void setCountry(LocalizableString country) {
    this.country = country;
  }

  public String getCountryIso() {
    return countryIso;
  }

  public void setCountryIso(String countryIso) {
    this.countryIso = countryIso;
  }
}
