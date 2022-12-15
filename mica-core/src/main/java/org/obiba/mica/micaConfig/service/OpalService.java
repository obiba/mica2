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
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.obiba.magma.support.Initialisables;
import org.obiba.mica.dataset.service.KeyStoreService;
import org.obiba.mica.micaConfig.AuthType;
import org.obiba.mica.micaConfig.domain.OpalCredential;
import org.obiba.mica.micaConfig.service.helper.OpalServiceHelper;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.rest.client.magma.OpalJavaClient;
import org.obiba.opal.rest.client.magma.RestDatasource;
import org.obiba.opal.rest.client.magma.RestDatasourceFactory;
import org.obiba.opal.web.model.Projects;
import org.obiba.opal.web.model.Search;
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
    opalUrl = Strings.isNullOrEmpty(opalUrl) ? getPrimaryOpal() : opalUrl;

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

  /**
   * Get the url of the default Opal server as defined in the configuration.
   *
   * @return
   */
  private String getPrimaryOpal() {
    String opalConf = micaConfigService.getConfig().getOpal();
    String opalDefault = environment.getProperty("opal.url");
    return Strings.isNullOrEmpty(opalConf) ? opalDefault : opalConf;
  }

  public boolean hasPrimaryOpal() {
    String primaryOpal = getPrimaryOpal();
    return !Strings.isNullOrEmpty(primaryOpal) && primaryOpal.toLowerCase().startsWith("http");
  }

  //
  // Opal Project
  //

  public List<Projects.ProjectDto> getProjectDtos(String opalUrl) throws URISyntaxException {
    if (Strings.isNullOrEmpty(opalUrl)) opalUrl = getPrimaryOpal();

    OpalJavaClient opalJavaClient = getOpalJavaClient(opalUrl);
    URI uri = opalJavaClient.newUri().segment("projects").build();

    return opalJavaClient.getResources(Projects.ProjectDto.class, uri, Projects.ProjectDto.newBuilder());
  }

  //
  // Entities count
  //

  public Search.EntitiesResultDto getEntitiesCount(String opalUrl, String query, String entityType) {
    try {
      return opalServiceHelper.getEntitiesCount(getOpalJavaClient(opalUrl), query, entityType);
    } catch (URISyntaxException e) {
      log.error("Malformed opal URI", e);
      throw new NoSuchElementException();
    }
  }

  //
  // Private/package methods
  //

  synchronized Map<String, Taxonomy> getTaxonomiesInternal() {
    if (hasPrimaryOpal()) {
      try {
        return opalServiceHelper.getTaxonomies(getOpalJavaClient());
      } catch (Exception e) {
        log.error("Cannot retrieve Opal taxonomies", e);
        throw new NoSuchElementException();
      }
    } else {
      return Maps.newHashMap();
    }
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

  private String getOpalProjectUrl(String opalUrl, String project) {
    String baseUrl = opalUrl == null ? getPrimaryOpal() : opalUrl;

    return String.format("%s/ws/datasource/%s", StringUtils.stripEnd(baseUrl, "/"), project);
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
      opalJavaClient = new OpalJavaClient(cleanupOpalUrl(getPrimaryOpal()), getOpalUsername(), getOpalPassword());
    else
      opalJavaClient = new OpalJavaClient(cleanupOpalUrl(getPrimaryOpal()), getOpalToken());

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
      return new OpalCredential(getPrimaryOpal(), AuthType.USERNAME, getOpalUsername(), getOpalPassword());
    else
      return new OpalCredential(getPrimaryOpal(), AuthType.TOKEN, getOpalToken());
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
