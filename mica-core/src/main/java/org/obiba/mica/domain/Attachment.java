package org.obiba.mica.domain;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import javax.validation.constraints.NotNull;

public class Attachment implements Serializable {

  private static final long serialVersionUID = 7881381748865114007L;

  @NotNull
  private String name;

  private String type;

  private LocalizedString description;

  private Locale locale;

  private Locale lang;

  private Integer size;

  private String md5;

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

  public Locale getLang() {
    return lang;
  }

  public void setLang(Locale lang) {
    this.lang = lang;
  }

  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

  @Override
  public int hashCode() {return Objects.hash(name, type, description, locale, lang, size, md5);}

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Attachment other = (Attachment) obj;
    return Objects.equals(name, other.name) && Objects.equals(type, other.type) &&
        Objects.equals(description, other.description) && Objects.equals(locale, other.locale) &&
        Objects.equals(lang, other.lang) && Objects.equals(size, other.size) &&
        Objects.equals(md5, other.md5);
  }
}
