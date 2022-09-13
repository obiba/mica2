/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.obiba.magma.support.Initialisables;
import org.obiba.mica.dataset.service.KeyStoreService;
import org.obiba.mica.micaConfig.AuthType;
import org.obiba.mica.micaConfig.domain.OpalCredential;
import org.obiba.mica.micaConfig.service.helper.OpalServiceHelper;
import org.obiba.opal.core.cfg.NoSuchTaxonomyException;
import org.obiba.opal.core.cfg.NoSuchVocabularyException;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.opal.rest.client.magma.OpalJavaClient;
import org.obiba.opal.rest.client.magma.RestDatasource;
import org.obiba.opal.rest.client.magma.RestDatasourceFactory;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.model.Projects;
import org.obiba.opal.web.model.Search;
import org.obiba.opal.web.taxonomy.Dtos;
import org.obiba.security.KeyStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OpalService implements EnvironmentAware {

  public static final String OPAL_KEYSTORE = "opal";

  private static final Logger log = LoggerFactory.getLogger(OpalService.class);

  private Map<String, Pair<OpalCredential, RestDatasource>> cachedDatasources = new HashMap<>();

  private Environment environment;

  private OpalJavaClient opalJavaClient;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private KeyStoreService keyStoreService;

  @Inject
  private OpalCredentialService opalCredentialService;

  @Inject
  private OpalServiceHelper opalServiceHelper;

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  /**
   * Get the datasource from the provided Opal server url.
   *
   * @param opalUrl if null, default Opal server url will be used.
   * @param project
   * @return
   */
  public synchronized RestDatasource getDatasource(@Nullable String opalUrl, String project) {
    final String projectUrl = getOpalProjectUrl(opalUrl, project);
    opalUrl = Strings.isNullOrEmpty(opalUrl) ? getDefaultOpal() : opalUrl;

    OpalCredential opalCredential = getOpalCredential(opalUrl);

    if (cachedDatasources.containsKey(projectUrl)) {
      Pair<OpalCredential, RestDatasource> p = cachedDatasources.get(projectUrl);

      if (p.getKey().equals(opalCredential)) {
        log.debug("Using cached rest datasource to " + projectUrl);
        return p.getValue();
      }

      log.debug("Opal credential changed, evicting rest datasource for " + projectUrl);

      cachedDatasources.remove(projectUrl); //opal credential changed
    }

    RestDatasource datasource = createRestDatasource(opalCredential, projectUrl, opalUrl, project);
    Initialisables.initialise(datasource);
    cachedDatasources.put(projectUrl, Pair.create(opalCredential, datasource));

    log.debug("Initialized rest datasource for " + projectUrl);

    return datasource;
  }

  private RestDatasource createRestDatasource(OpalCredential opalCredential, String projectUrl, String opalUrl,
                                              String project) {
    if (opalCredential.getAuthType() == AuthType.CERTIFICATE) {
      KeyStoreManager kms = keyStoreService.getKeyStore(OPAL_KEYSTORE);

      if (!kms.aliasExists(opalCredential.getOpalUrl())) throw new IllegalStateException(
        "Trying to use opal certificate credential but could not be found in keystore.");

      return (RestDatasource) new RestDatasourceFactory(projectUrl, opalUrl, kms.getKeyStore(), opalUrl,
        micaConfigService.getConfig().getSecretKey(), project).create();
    } else if (opalCredential.getAuthType() == AuthType.TOKEN)
      return (RestDatasource) new RestDatasourceFactory(projectUrl, opalUrl, opalCredential.getToken(), project).create();

    return (RestDatasource) new RestDatasourceFactory(projectUrl, opalUrl, opalCredential.getUsername(),
      opalCredential.getPassword(), project).create();
  }

  /**
   * Get a {@link RestDatasource} from the default Opal server.
   *
   * @param project
   * @return
   */
  public RestDatasource getDatasource(String project) {
    return getDatasource(getDefaultOpal(), project);
  }

  private String getOpalProjectUrl(String opalUrl, String project) {
    String baseUrl = opalUrl == null ? getDefaultOpal() : opalUrl;

    return String.format("%s/ws/datasource/%s", StringUtils.stripEnd(baseUrl, "/"), project);
  }

  /**
   * Get the url of the default Opal server as defined in the configuration.
   *
   * @return
   */
  public String getDefaultOpal() {
    return environment.getProperty("opal.url");
  }

  //
  // Taxonomies
  //

  public List<Taxonomy> getTaxonomies() {
    Map<String, Taxonomy> taxonomies = getTaxonomiesInternal();
    List<Taxonomy> taxonomyList = Lists.newArrayList(taxonomies.values());
    Collections.sort(taxonomyList, Comparator.comparing(TaxonomyEntity::getName));
    return taxonomyList;
  }

  public List<Opal.TaxonomyDto> getTaxonomyDtos() {
    return getTaxonomies().stream().map(Dtos::asDto).collect(Collectors.toList());
  }

  /**
   * Get a summary of all the {@link Taxonomy}s available from Opal master.
   *
   * @return
   */
  public Opal.TaxonomiesDto getTaxonomySummaryDtos() {
    List<Opal.TaxonomiesDto.TaxonomySummaryDto> summaries = getTaxonomies().stream().map(Dtos::asSummaryDto)
      .collect(Collectors.toList());

    return Opal.TaxonomiesDto.newBuilder().addAllSummaries(summaries).build();
  }

  /**
   * Get a summary of the {@link Taxonomy} available from Opal master.
   *
   * @param name the taxonomy name
   * @return
   */
  public Opal.TaxonomiesDto.TaxonomySummaryDto getTaxonomySummaryDto(String name) {
    return Dtos.asSummaryDto(getTaxonomy(name));
  }

  /**
   * Get a summary of all the {@link Taxonomy}s with their
   * {@link Vocabulary}s from Opal master.
   *
   * @return
   */
  public Opal.TaxonomiesDto getTaxonomyVocabularySummaryDtos() {
    List<Opal.TaxonomiesDto.TaxonomySummaryDto> summaries = getTaxonomies().stream().map(Dtos::asVocabularySummaryDto)
      .collect(Collectors.toList());

    return Opal.TaxonomiesDto.newBuilder().addAllSummaries(summaries).build();
  }

  /**
   * Get a summary of the {@link Taxonomy} with its
   * {@link Vocabulary}s from Opal master.
   *
   * @param name the taxonomy name
   * @return
   */
  public Opal.TaxonomiesDto.TaxonomySummaryDto getTaxonomyVocabularySummaryDto(String name) {
    return Dtos.asVocabularySummaryDto(getTaxonomy(name));
  }

  /**
   * Get a summary of the {@link Vocabulary} from Opal master.
   *
   * @param name
   * @param vocabularyName
   * @return
   */
  public Opal.TaxonomiesDto.TaxonomySummaryDto.VocabularySummaryDto getTaxonomyVocabularySummaryDto(String name,
                                                                                                    String vocabularyName) {
    for (Vocabulary voc : getTaxonomy(name).getVocabularies()) {
      if (voc.getName().equals(vocabularyName)) return Dtos.asSummaryDto(voc);
    }
    throw new NoSuchVocabularyException(name, vocabularyName);
  }

  /**
   * Get the {@link Taxonomy} from Opal master.
   *
   * @param name
   * @return
   * @throws NoSuchTaxonomyException
   */
  public Taxonomy getTaxonomy(String name) {
    return Dtos.fromDto(getTaxonomyDto(name));
  }

  /**
   * Get the {@link Taxonomy} as a Dto from Opal master.
   *
   * @param name
   * @return
   * @throws NoSuchTaxonomyException
   */
  public Opal.TaxonomyDto getTaxonomyDto(String name) {
    Map<String, Taxonomy> taxonomies = getTaxonomiesInternal();

    if (!taxonomies.containsKey(name)) {
      throw new NoSuchTaxonomyException(name);
    }

    return Dtos.asDto(taxonomies.get(name));
  }

  /**
   * Get the {@link Vocabulary} as a Dto from Opal master.
   *
   * @param name
   * @param vocabularyName
   * @return
   */
  public Opal.VocabularyDto getTaxonomyVocabularyDto(String name, String vocabularyName) {
    Map<String, Taxonomy> taxonomies = getTaxonomiesInternal();

    if (!taxonomies.containsKey(name)) {
      throw new NoSuchTaxonomyException(name);
    }

    return Dtos.asDto(taxonomies.get(name).getVocabulary(vocabularyName));
  }

  public List<Projects.ProjectDto> getProjectDtos(String opalUrl) throws URISyntaxException {
    if (Strings.isNullOrEmpty(opalUrl)) opalUrl = getDefaultOpal();

    OpalJavaClient opalJavaClient = getOpalJavaClient(opalUrl);
    URI uri = opalJavaClient.newUri().segment("projects").build();

    return opalJavaClient.getResources(Projects.ProjectDto.class, uri, Projects.ProjectDto.newBuilder());
  }

  //
  // Private methods
  //

  private synchronized Map<String, Taxonomy> getTaxonomiesInternal() {
    try {
      return opalServiceHelper.getTaxonomies(getOpalJavaClient());
    } catch (URISyntaxException e) {
      log.error("Malformed opal URI", e);
      throw new NoSuchElementException();
    }
  }

  public Search.EntitiesResultDto getEntitiesCount(String query, String entityType) {
    return getEntitiesCount(getDefaultOpal(), query, entityType);
  }

  public Search.EntitiesResultDto getEntitiesCount(String opalUrl, String query, String entityType) {
    try {
      return opalServiceHelper.getEntitiesCount(getOpalJavaClient(opalUrl), query, entityType);
    } catch (URISyntaxException e) {
      log.error("Malformed opal URI", e);
      throw new NoSuchElementException();
    }
  }

  private String getOpalUsername() {
    return environment.getProperty("opal.username");
  }

  private String getOpalPassword() {
    return environment.getProperty("opal.password");
  }

  private String getOpalToken() {
    return environment.getProperty("opal.token");
  }

  private OpalJavaClient getOpalJavaClient() throws URISyntaxException {
    if (opalJavaClient != null) return opalJavaClient;

    if (Strings.isNullOrEmpty(getOpalToken()))
      opalJavaClient = new OpalJavaClient(cleanupOpalUrl(getDefaultOpal()), getOpalUsername(), getOpalPassword());
    else
      opalJavaClient = new OpalJavaClient(cleanupOpalUrl(getDefaultOpal()), getOpalToken());

    return opalJavaClient;
  }

  private OpalJavaClient getOpalJavaClient(String opalUrl) throws URISyntaxException {
    String alias = opalUrl;
    OpalCredential opalCredential = getOpalCredential(opalUrl);

    if (opalCredential.getAuthType() == AuthType.CERTIFICATE) {
      KeyStoreManager kms = keyStoreService.getKeyStore(OPAL_KEYSTORE);

      if (!kms.aliasExists(alias)) throw new IllegalStateException(
        "Trying to use opal certificate credential but could not be found in keystore.");

      return new OpalJavaClient(cleanupOpalUrl(opalUrl), kms.getKeyStore(), alias,
        micaConfigService.getConfig().getSecretKey());
    } else if (opalCredential.getAuthType() == AuthType.TOKEN)
      return new OpalJavaClient(cleanupOpalUrl(opalCredential.getOpalUrl()), opalCredential.getToken());

    return new OpalJavaClient(cleanupOpalUrl(opalCredential.getOpalUrl()), opalCredential.getUsername(), opalCredential.getPassword());
  }

  private OpalCredential getOpalCredential(String opalUrl) {
    Optional<OpalCredential> opalCredential = opalCredentialService.findOpalCredentialById(opalUrl);
    if (opalCredential.isPresent()) return opalCredential.get();

    if (Strings.isNullOrEmpty(getOpalToken()))
      return new OpalCredential(getDefaultOpal(), AuthType.USERNAME, getOpalUsername(), getOpalPassword());
    else
      return new OpalCredential(getDefaultOpal(), AuthType.TOKEN, getOpalToken());
  }

  private String cleanupOpalUrl(String opalUrl) {
    while (opalUrl.endsWith("/")) {
      opalUrl = opalUrl.substring(0, opalUrl.length() - 1);
    }

    if (!opalUrl.endsWith("/ws")) {
      opalUrl = opalUrl + "/ws";
    }

    return opalUrl;
  }
}
