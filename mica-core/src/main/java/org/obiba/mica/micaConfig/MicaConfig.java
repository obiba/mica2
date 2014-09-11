package org.obiba.mica.micaConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.springframework.data.mongodb.core.mapping.Document;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Document
public class MicaConfig extends AbstractAuditableDocument {

  private static final long serialVersionUID = -9020464712632680519L;

  public static final String DEFAULT_NAME = "Mica";

  public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  public static final String DEFAULT_CHARSET = Charsets.UTF_8.toString();

  public static final String DEFAULT_OPAL = "https://localhost:8443";

  @NotBlank
  private String name = DEFAULT_NAME;

  @NotEmpty
  private List<Locale> locales = new ArrayList<>();

  @NotBlank
  private String defaultCharacterSet = DEFAULT_CHARSET;

  @NotBlank
  private String opal = DEFAULT_OPAL;

  private String publicUrl;

  private String secretKey;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Locale> getLocales() {
    return locales == null ? (locales = new ArrayList<>()) : locales;
  }

  public List<String> getLocalesAsString() {
    List<String> list = Lists.newArrayList(Iterables.transform(getLocales(), Locale::getLanguage));
    Collections.sort(list);
    return list;
  }

  public void setLocales(List<Locale> locales) {
    this.locales = locales;
  }

  public String getDefaultCharacterSet() {
    return defaultCharacterSet;
  }

  public void setDefaultCharacterSet(String defaultCharacterSet) {
    this.defaultCharacterSet = defaultCharacterSet;
  }

  public String getOpal() {
    return opal;
  }

  public void setOpal(String opal) {
    this.opal = opal;
  }

  public String getPublicUrl() {
    return publicUrl;
  }

  public void setPublicUrl(String publicUrl) {
    this.publicUrl = publicUrl;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  @Override
  protected Objects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("name", name) //
        .add("locales", locales) //
        .add("defaultCharacterSet", defaultCharacterSet) //
        .add("publicUrl", publicUrl);
  }
}
