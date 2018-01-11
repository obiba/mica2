/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

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
