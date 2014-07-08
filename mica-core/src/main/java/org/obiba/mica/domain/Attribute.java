package org.obiba.mica.domain;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class Attribute implements Serializable {

  private static final long serialVersionUID = 8869937335553092873L;

  @NotNull
  private String name;

  private String namespace;

  private Locale locale;

  @NotNull
  private String value;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  @JsonIgnore
  public boolean isLocalised() {
    return locale != null;
  }

  public boolean isLocalisedWith(@SuppressWarnings("ParameterHidesMemberVariable") @Nullable Locale locale) {
    return locale == null && !isLocalised() || locale != null && isLocalised() && locale.equals(getLocale());
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @JsonIgnore
  public String getMapKey() {
    return getMapKey(name, namespace);
  }

  public static String getMapKey(String name, @Nullable String namespace) {
    return Strings.isNullOrEmpty(namespace) ? name : namespace + "." + name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, namespace, locale, value);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null || getClass() != obj.getClass()) return false;
    Attribute other = (Attribute) obj;
    return Objects.equals(name, other.name) && Objects.equals(namespace, other.namespace) &&
        Objects.equals(locale, other.locale) && Objects.equals(value, other.value);
  }

  @Override
  public String toString() {
    return com.google.common.base.Objects.toStringHelper(this).omitNullValues().add("name", name)
        .add("namespace", namespace).add("locale", locale).add("value", value).toString();
  }

  @SuppressWarnings("ParameterHidesMemberVariable")
  public static class Builder {

    private final Attribute attribute;

    private Builder(@NotNull String name) {
      Preconditions.checkArgument(name != null, "name cannot be null");
      attribute = new Attribute();
      attribute.setName(name);
    }

    private Builder() {
      attribute = new Attribute();
    }

    public static Builder newAttribute(String name) {
      return new Builder(name);
    }

    public static Builder newAttribute(org.obiba.magma.Attribute attr) {
      Builder builder = new Builder(attr.getName());
      if(attr.hasNamespace()) builder.namespace(attr.getNamespace());
      if(attr.isLocalised()) builder.locale(attr.getLocale());
      if(attr.getValue() != null) builder.value(attr.getValue().toString());
      return builder;
    }

    public Builder namespace(String namespace) {
      attribute.namespace = namespace;
      return this;
    }

    public Builder value(String value) {
      attribute.value = value;
      return this;
    }

    public Builder locale(Locale locale) {
      attribute.locale = locale;
      return this;
    }

    public Attribute build() {
      return attribute;
    }

  }
}
