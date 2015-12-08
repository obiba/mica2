package org.obiba.mica.micaConfig.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.domain.Membership;
import org.obiba.runtime.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
  private List<Locale> locales = Lists.newArrayList();

  @NotBlank
  private String defaultCharacterSet = DEFAULT_CHARSET;

  @NotBlank
  private String opal = DEFAULT_OPAL;

  private AggregationsConfig aggregations;

  private List<String> roles = Lists.newArrayList(Membership.CONTACT, Membership.INVESTIGATOR);

  private String publicUrl;

  private String secretKey;

  private Version micaVersion;

  private int privacyThreshold = 0;

  private boolean openAccess = true;

  private boolean isFsNotificationsEnabled = false;

  private String fsNotificationsSubject;

  private boolean isCommentNotificationsEnabled = false;

  private String commentNotiticationsSubject;

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

  public boolean hasPublicUrl() {
    return !Strings.isNullOrEmpty(publicUrl);
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  @Override
  protected MoreObjects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("name", name) //
        .add("locales", locales) //
        .add("defaultCharacterSet", defaultCharacterSet) //
        .add("publicUrl", publicUrl);
  }

  public AggregationsConfig getAggregations() {
    return aggregations;
  }

  public void setAggregations(AggregationsConfig aggregations) {
    this.aggregations = aggregations;
  }

  public Version getMicaVersion() {
    return micaVersion;
  }

  public void setMicaVersion(Version micaVersion) {
    this.micaVersion = micaVersion;
  }

  public int getPrivacyThreshold() {
    return privacyThreshold;
  }

  public void setPrivacyThreshold(int privacyThreshold) {
    this.privacyThreshold = privacyThreshold;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles == null ? Lists.newArrayList() : Lists.newArrayList(Sets.newLinkedHashSet(roles));
  }

  public void setOpenAccess(boolean openAccess) {
    this.openAccess = openAccess;
  }

  public boolean isOpenAccess() {
    return openAccess;
  }

  public boolean isFsNotificationsEnabled() {
    return isFsNotificationsEnabled;
  }

  public void setFsNotificationsEnabled(boolean isFsNotificationsEnabled) {
    this.isFsNotificationsEnabled = isFsNotificationsEnabled;
  }

  public String getFsNotificationSubject() {
    return fsNotificationsSubject;
  }

  public void setFsNotificationSubject(String fsNotificationSubject) {
    this.fsNotificationsSubject = fsNotificationSubject;
  }

  public boolean isCommentNotificationsEnabled() {
    return isCommentNotificationsEnabled;
  }

  public void setCommentNotificationsEnabled(boolean isCommentNotificationsEnabled) {
    this.isCommentNotificationsEnabled = isCommentNotificationsEnabled;
  }

  public String getCommentNotiticationsSubject() {
    return commentNotiticationsSubject;
  }

  public void setCommentNotiticationsSubject(String commentNotiticationsSubject) {
    this.commentNotiticationsSubject = commentNotiticationsSubject;
  }
}
