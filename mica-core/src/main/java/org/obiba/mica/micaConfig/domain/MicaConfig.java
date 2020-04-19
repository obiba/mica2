/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.Membership;
import org.obiba.runtime.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Document
public class MicaConfig extends AbstractAuditableDocument {

  private static final long serialVersionUID = -9020464712632680519L;

  public static final String DEFAULT_NAME = "Mica";

  public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  public static final String DEFAULT_CHARSET = Charsets.UTF_8.toString();

  public static final String DEFAULT_OPAL = "https://localhost:8443";

  public static final String DEFAULT_PUBLIC_URL = "http://localhost:8082";

  public static final String[] LAYOUT_OPTIONS = {"layout1", "layout2"};

  public static final long DEFAULT_MAX_ITEMS_PER_SET = 20000;

  public static final long DEFAULT_MAX_SETS = 10;

  public static final boolean ANONYMOUS_CAN_CREATE_CART = true;

  @NotBlank
  private String name = DEFAULT_NAME;

  @NotEmpty
  private List<Locale> locales = Lists.newArrayList();

  @NotBlank
  private String defaultCharacterSet = DEFAULT_CHARSET;

  @NotBlank
  private String opal = DEFAULT_OPAL;

  private List<String> roles = Lists.newArrayList(Membership.CONTACT, Membership.INVESTIGATOR);

  private String publicUrl;

  private String portalUrl;

  private String secretKey;

  private Version micaVersion;

  private int privacyThreshold = 0;

  private boolean openAccess = true;

  private boolean isStudyNotificationsEnabled = false;

  private String studyNotificationsSubject;

  private boolean isNetworkNotificationsEnabled = false;

  private String networkNotificationsSubject;

  private boolean isStudyDatasetNotificationsEnabled = false;

  private String studyDatasetNotificationsSubject;

  private boolean isHarmonizationDatasetNotificationsEnabled = false;

  private String harmonizationDatasetNotificationsSubject;

  private boolean isFsNotificationsEnabled = false;

  private String fsNotificationsSubject;

  private boolean isCommentNotificationsEnabled = false;

  private String commentNotiticationsSubject;

  private boolean isProjectNotificationsEnabled = false;

  private String projectNotificationsSubject;

  private boolean isRepositoryEnabled = true;

  private boolean isDataAccessEnabled = true;

  private boolean isProjectEnabled = true;

  private boolean isSingleStudyEnabled = false;

  private boolean isSingleNetworkEnabled = false;

  private boolean isNetworkEnabled = true;

  private boolean isStudyDatasetEnabled = true;

  private boolean isHarmonizationDatasetEnabled = true;

  private boolean variableSummaryRequiresAuthentication = false;

  private boolean usePublicUrlForSharedLink = true;

  private String style;

  private LocalizedString translations;

  private String searchLayout = "layout2";

  private long maxNumberOfSets = DEFAULT_MAX_SETS;

  private long maxItemsPerSet = DEFAULT_MAX_ITEMS_PER_SET;

  private boolean anonymousCanCreateCart = ANONYMOUS_CAN_CREATE_CART;

  private boolean cartEnabled = true;

  private int cartTimeToLive = 30; // 1 month

  private int setTimeToLive = 365; // year

  private boolean setsAnalysisEnabled = true;

  private boolean setsSearchEnabled = true;

  private boolean signupEnabled = true;

  private List<String> signupGroups = Lists.newArrayList("mica-user");

  private boolean signupWithPassword = true;

  private OpalViewsGrouping opalViewsGrouping = OpalViewsGrouping.PROJECT_TABLE;

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
    return getLocales().stream().map(Locale::getLanguage).sorted().collect(Collectors.toList());
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

  public String getPortalUrl() {
    return portalUrl;
  }

  public void setPortalUrl(String portalUrl) {
    this.portalUrl = portalUrl;
  }

  public void setUsePublicUrlForSharedLink(boolean usePublicUrlForSharedLink) {
    this.usePublicUrlForSharedLink = usePublicUrlForSharedLink;
  }

  public boolean isUsePublicUrlForSharedLink() {
    return usePublicUrlForSharedLink;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  @Override
  protected MoreObjects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("name", name)
      .add("locales", locales)
      .add("defaultCharacterSet", defaultCharacterSet)
      .add("publicUrl", publicUrl)
      .add("portalUrl", portalUrl);
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

  public boolean isSignupEnabled() {
    return signupEnabled;
  }

  public void setSignupEnabled(boolean signupEnabled) {
    this.signupEnabled = signupEnabled;
  }

  public void setSignupGroups(List<String> signupGroups) {
    this.signupGroups = signupGroups == null || signupGroups.isEmpty() ? Lists.newArrayList("mica-user") : Lists.newArrayList(Sets.newLinkedHashSet(signupGroups));
  }

  public List<String> getSignupGroups() {
    return signupGroups;
  }

  public void setSignupWithPassword(boolean signupWithPassword) {
    this.signupWithPassword = signupWithPassword;
  }

  public boolean isSignupWithPassword() {
    return signupWithPassword;
  }

  public void setOpenAccess(boolean openAccess) {
    this.openAccess = openAccess;
  }

  public boolean isOpenAccess() {
    return openAccess;
  }

  public boolean isStudyNotificationsEnabled() {
    return isStudyNotificationsEnabled;
  }

  public void setStudyNotificationsEnabled(boolean isStudyNotificationsEnabled) {
    this.isStudyNotificationsEnabled = isStudyNotificationsEnabled;
  }

  public String getStudyNotificationsSubject() {
    return studyNotificationsSubject;
  }

  public void setStudyNotificationsSubject(String studyNotificationsSubject) {
    this.studyNotificationsSubject = studyNotificationsSubject;
  }

  public boolean isNetworkNotificationsEnabled() {
    return isNetworkNotificationsEnabled;
  }

  public void setNetworkNotificationsEnabled(boolean isNetworkNotificationsEnabled) {
    this.isNetworkNotificationsEnabled = isNetworkNotificationsEnabled;
  }

  public String getNetworkNotificationsSubject() {
    return networkNotificationsSubject;
  }

  public void setNetworkNotificationsSubject(String networkNotificationsSubject) {
    this.networkNotificationsSubject = networkNotificationsSubject;
  }

  public boolean isStudyDatasetNotificationsEnabled() {
    return isStudyDatasetNotificationsEnabled;
  }

  public void setStudyDatasetNotificationsEnabled(boolean isStudyDatasetNotificationsEnabled) {
    this.isStudyDatasetNotificationsEnabled = isStudyDatasetNotificationsEnabled;
  }

  public String getStudyDatasetNotificationsSubject() {
    return studyDatasetNotificationsSubject;
  }

  public void setStudyDatasetNotificationsSubject(String studyDatasetNotificationsSubject) {
    this.studyDatasetNotificationsSubject = studyDatasetNotificationsSubject;
  }

  public boolean isHarmonizationDatasetNotificationsEnabled() {
    return isHarmonizationDatasetNotificationsEnabled;
  }

  public void setHarmonizationDatasetNotificationsEnabled(boolean isHarmonizationDatasetNotificationsEnabled) {
    this.isHarmonizationDatasetNotificationsEnabled = isHarmonizationDatasetNotificationsEnabled;
  }

  public String getHarmonizationDatasetNotificationsSubject() {
    return harmonizationDatasetNotificationsSubject;
  }

  public void setHarmonizationDatasetNotificationsSubject(String harmonizationDatasetNotificationsSubject) {
    this.harmonizationDatasetNotificationsSubject = harmonizationDatasetNotificationsSubject;
  }

  public boolean isFsNotificationsEnabled() {
    return isFsNotificationsEnabled;
  }

  public void setFsNotificationsEnabled(boolean isFsNotificationsEnabled) {
    this.isFsNotificationsEnabled = isFsNotificationsEnabled;
  }

  public String getFsNotificationsSubject() {
    return fsNotificationsSubject;
  }

  public void setFsNotificationsSubject(String fsNotificationsSubject) {
    this.fsNotificationsSubject = fsNotificationsSubject;
  }

  public boolean isCommentNotificationsEnabled() {
    return isCommentNotificationsEnabled;
  }

  public void setCommentNotificationsEnabled(boolean isCommentNotificationsEnabled) {
    this.isCommentNotificationsEnabled = isCommentNotificationsEnabled;
  }

  public String getCommentNotificationsSubject() {
    return commentNotiticationsSubject;
  }

  public void setCommentNotificationsSubject(String commentNotificationsSubject) {
    commentNotiticationsSubject = commentNotificationsSubject;
  }

  public boolean isProjectNotificationsEnabled() {
    return isProjectNotificationsEnabled;
  }

  public void setProjectNotificationsEnabled(boolean projectNotificationsEnabled) {
    isProjectNotificationsEnabled = projectNotificationsEnabled;
  }

  public String getProjectNotificationsSubject() {
    return projectNotificationsSubject;
  }

  public void setProjectNotificationsSubject(String projectNotificationsSubject) {
    this.projectNotificationsSubject = projectNotificationsSubject;
  }

  public boolean isRepositoryEnabled() {
    return isRepositoryEnabled;
  }

  public void setRepositoryEnabled(boolean repositoryEnabled) {
    isRepositoryEnabled = repositoryEnabled;
  }

  public boolean isDataAccessEnabled() {
    return isDataAccessEnabled;
  }

  public void setDataAccessEnabled(boolean dataAccessEnabled) {
    isDataAccessEnabled = dataAccessEnabled;
  }

  public boolean isProjectEnabled() {
    return isProjectEnabled;
  }

  public void setProjectEnabled(boolean projectEnabled) {
    isProjectEnabled = projectEnabled;
  }

  public boolean isSingleStudyEnabled() {
    return isSingleStudyEnabled;
  }

  public void setSingleStudyEnabled(boolean addStudyEnabled) {
    isSingleStudyEnabled = addStudyEnabled;
  }

  public boolean isSingleNetworkEnabled() {
    return isSingleNetworkEnabled;
  }

  public void setSingleNetworkEnabled(boolean addNetworkEnabled) {
    isSingleNetworkEnabled = addNetworkEnabled;
  }

  public boolean isNetworkEnabled() {
    return isNetworkEnabled;
  }

  public void setNetworkEnabled(boolean networkEnabled) {
    isNetworkEnabled = networkEnabled;
  }

  public boolean isStudyDatasetEnabled() {
    return isStudyDatasetEnabled;
  }

  public void setStudyDatasetEnabled(boolean studyDatasetEnabled) {
    isStudyDatasetEnabled = studyDatasetEnabled;
  }

  public boolean isHarmonizationDatasetEnabled() {
    return isHarmonizationDatasetEnabled;
  }

  public void setHarmonizationDatasetEnabled(boolean harmonizationDatasetEnabled) {
    isHarmonizationDatasetEnabled = harmonizationDatasetEnabled;
  }

  public boolean isVariableSummaryRequiresAuthentication() {
    return variableSummaryRequiresAuthentication;
  }

  public void setVariableSummaryRequiresAuthentication(boolean variableSummaryRequiresAuthentication) {
    this.variableSummaryRequiresAuthentication = variableSummaryRequiresAuthentication;
  }

  public boolean hasStyle() {
    return !Strings.isNullOrEmpty(style);
  }

  public void setStyle(String style) {
    this.style = style;
  }

  public String getStyle() {
    return style;
  }

  public LocalizedString getTranslations() {
    return translations;
  }

  public void setTranslations(LocalizedString translations) {
    this.translations = translations;
  }

  public boolean hasTranslations() {
    return translations != null && !translations.isEmpty();
  }

  public String getSearchLayout() {
    return searchLayout;
  }

  public void setSearchLayout(String searchLayout) {
    this.searchLayout = searchLayout;
  }

  public long getMaxNumberOfSets() {
    return maxNumberOfSets;
  }

  public void setMaxNumberOfSets(long maxNumberOfSets) {
    this.maxNumberOfSets = maxNumberOfSets;
  }

  public long getMaxItemsPerSet() {
    return maxItemsPerSet;
  }

  public void setMaxItemsPerSet(long maxItemsPerSet) {
    this.maxItemsPerSet = maxItemsPerSet;
  }

  public boolean isAnonymousCanCreateCart() {
    return anonymousCanCreateCart;
  }

  public void setAnonymousCanCreateCart(boolean anonymousCanCreateCart) {
    this.anonymousCanCreateCart = anonymousCanCreateCart;
  }

  public boolean isCartEnabled() {
    return cartEnabled;
  }

  public void setCartEnabled(boolean cartEnabled) {
    this.cartEnabled = cartEnabled;
  }

  public int getCartTimeToLive() {
    return cartTimeToLive;
  }

  public void setCartTimeToLive(int cartTimeToLive) {
    this.cartTimeToLive = cartTimeToLive;
  }

  public int getSetTimeToLive() {
    return setTimeToLive;
  }

  public void setSetTimeToLive(int setTimeToLive) {
    this.setTimeToLive = setTimeToLive;
  }

  public boolean isSetsAnalysisEnabled() {
    return setsAnalysisEnabled;
  }

  public void setSetsAnalysisEnabled(boolean setsAnalysisEnabled) {
    this.setsAnalysisEnabled = setsAnalysisEnabled;
  }

  public boolean isSetsSearchEnabled() {
    return setsSearchEnabled;
  }

  public void setSetsSearchEnabled(boolean setsSearchEnabled) {
    this.setsSearchEnabled = setsSearchEnabled;
  }

  public OpalViewsGrouping getOpalViewsGrouping() {
    return opalViewsGrouping;
  }

  public void setOpalViewsGrouping(OpalViewsGrouping opalViewsGrouping) {
    this.opalViewsGrouping = opalViewsGrouping;
  }

  public enum OpalViewsGrouping {
    PROJECT_TABLE,
    PROJECT_ENTITY_TYPE,
    ENTITY_TYPE
  }
}
