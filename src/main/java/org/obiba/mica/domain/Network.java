package org.obiba.mica.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;
import org.obiba.mica.domain.util.CustomLocalDateSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.deser.LocalDateDeserializer;

/**
 * A Network.
 */
@Entity
@Table(name = "T_NETWORK")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Network implements Serializable {

  private static final long serialVersionUID = -4271967393906681773L;

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE)
  private long id;

  @Size(min = 1, max = 50)
  @Column(name = "sample_text_attribute")
  private String sampleTextAttribute;

  @NotNull
  @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDate")
  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonSerialize(using = CustomLocalDateSerializer.class)
  @Column(name = "sample_date_attribute")
  private LocalDate sampleDateAttribute;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getSampleTextAttribute() {
    return sampleTextAttribute;
  }

  public void setSampleTextAttribute(String sampleTextAttribute) {
    this.sampleTextAttribute = sampleTextAttribute;
  }

  public LocalDate getSampleDateAttribute() {
    return sampleDateAttribute;
  }

  public void setSampleDateAttribute(LocalDate sampleDateAttribute) {
    this.sampleDateAttribute = sampleDateAttribute;
  }

  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || getClass() != o.getClass()) {
      return false;
    }

    Network network = (Network) o;

    if(id != network.id) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return (int) (id ^ id >>> 32);
  }

  @Override
  public String toString() {
    return "Network{" +
        "id=" + id +
        ", sampleTextAttribute='" + sampleTextAttribute + '\'' +
        ", sampleDateAttribute=" + sampleDateAttribute +
        '}';
  }
}
