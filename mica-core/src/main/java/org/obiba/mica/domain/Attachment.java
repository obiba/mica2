package org.obiba.mica.domain;

import java.io.Serializable;
import java.util.Locale;

import javax.validation.constraints.NotNull;

public class Attachment implements Serializable {

  private static final long serialVersionUID = 7881381748865114007L;

  @NotNull
  private String name;

  private String type;

  private LocalizedString description;

  private Locale locale;

  @NotNull
  public String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public LocalizedString getDescription() {
    return description;
  }

  public void setDescription(LocalizedString description) {
    this.description = description;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }
}
