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
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.obiba.core.util.FileUtil;
import org.obiba.mica.core.upgrade.RuntimeVersionProvider;
import org.obiba.mica.spi.search.ConfigurationProvider;
import org.obiba.mica.spi.search.SearchEngineService;
import org.obiba.mica.spi.search.SearchEngineServiceLoader;
import org.obiba.mica.spi.tables.StudyTableSourceService;
import org.obiba.mica.spi.tables.StudyTableSourceServiceLoader;
import org.obiba.mica.spi.taxonomies.TaxonomiesProviderService;
import org.obiba.mica.spi.taxonomies.TaxonomiesProviderServiceLoader;
import org.obiba.plugins.*;
import org.obiba.plugins.spi.ServicePlugin;
import org.obiba.runtime.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PluginsService implements EnvironmentAware, InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(PluginsService.class);

  private static final String PLUGINS_PATH = "${MICA_HOME}/plugins";

  private static final String MICA_SEARCH_PLUGIN_TYPE = "mica-search";

  private static final String MICA_TABLES_PLUGIN_TYPE = "mica-tables";

  private static final String MICA_TAXONOMIES_PLUGIN_TYPE = "mica-taxonomies";



  private static final String MICA_SEARCH_PLUGIN_NAME = "plugins.micaSearchPlugin";

  private static final String DEFAULT_MICA_SEARCH_PLUGIN_NAME = "mica-search-es8";

  private static final String DEFAULT_PLUGINS_UPDATE_SITE = "https://plugins.obiba.org";

  private static final String[] ES_CONFIGURATION = new String[]{"elasticsearch.dataNode", "elasticsearch.clusterName", "elasticsearch.shards", "elasticsearch.replicas",
      "elasticsearch.settings", "elasticsearch.maxConcurrentJoinQueries", "elasticsearch.concurrentJoinQueriesWaitTimeout",
      "elasticsearch.transportClient", "elasticsearch.transportAddress", "elasticsearch.transportSniff"};

  @Inject
  private ConfigurationProvider configurationProvider;

  @Inject
  private RuntimeVersionProvider runtimeVersionProvider;

  private File pluginsDir;

  private File archiveDir;

  private Collection<PluginResources> registeredPlugins;

  private List<ServicePlugin> servicePlugins = Lists.newArrayList();

  private Environment environment;

  private PluginRepositoryCache pluginRepositoryCache;

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  public SearchEngineService getSearchEngineService() {
    return (SearchEngineService) getServicePlugins(SearchEngineService.class).iterator().next();
  }

  public Collection<StudyTableSourceService> getStudyTableSourceServices() {
    return getServicePlugins(StudyTableSourceService.class).stream()
      .map(service -> (StudyTableSourceService)service)
      .collect(Collectors.toList());
  }

  public Collection<TaxonomiesProviderService> getTaxonomiesProviderServices() {
    return getServicePlugins(TaxonomiesProviderService.class).stream()
      .map(service -> (TaxonomiesProviderService)service)
      .collect(Collectors.toList());
  }

  /**
   * Get the location of the plugin packages repository.
   *
   * @return
   */
  public String getUpdateSite() {
    return environment.getProperty("plugins.updateSite", DEFAULT_PLUGINS_UPDATE_SITE);
  }

  /**
   * Get the last time at which the update site was successfully.
   *
   * @return
   */
  public Date getLastUpdate() {
    return getPluginRepositoryCache().getLastUpdate();
  }

  /**
   * Reports if system restart is required to finalize plugin installation.
   *
   * @return
   */
  public boolean restartRequired() {
    File[] children = pluginsDir.listFiles(pathname -> !pathname.getName().startsWith("."));
    if (children == null || children.length == 0) return false;
    for (File child : children) {
      if (child.isFile() && child.getName().endsWith(PluginResources.PLUGIN_DIST_SUFFIX)) return true;
      if (child.isDirectory() && new File(child, PluginResources.UNINSTALL_FILE).exists()) return true;
    }
    return false;
  }

  /**
   * Get the plugins registered in the system.
   *
   * @return
   */
  public List<PluginPackage> getInstalledPlugins() {
    return registeredPlugins.stream()
      .map(PluginPackage::new)
      .collect(Collectors.toList());
  }

  /**
   * Get the list of plugins that are marked to uninstallation.
   *
   * @return
   */
  public Collection<String> getUninstalledPluginNames() {
    List<String> names = Lists.newArrayList();
    File[] children = pluginsDir.listFiles(pathname -> pathname.isDirectory() && !pathname.getName().startsWith("."));
    if (children == null || children.length == 0) return names;
    for (File child : children) {
      PluginResources plugin = new MicaPlugin(child);
      if (plugin.isToUninstall()) names.add(plugin.getName());
    }
    return names;
  }

  /**
   * Get the plugins registered in the system that can be updated according to the update site registry.
   *
   * @return
   */
  public List<PluginPackage> getUpdatablePlugins() {
    // exclude already installed plugin packages whatever the version is
    return getPluginRepositoryCache().getOrUpdatePluginRepository().getPlugins().stream()
      .filter(PluginPackage::hasMicaVersion)
      .filter(pp -> registeredPlugins.stream().anyMatch(rp -> pp.isNewerThan(rp.getName(), rp.getVersion())))
      .filter(pp -> runtimeVersionProvider.getVersion().compareTo(pp.getMicaVersion()) >= 0)
      .collect(Collectors.toList());
  }

  /**
   * Get the plugins that are not installed and that are available from the update site registry.
   *
   * @return
   */
  public List<PluginPackage> getAvailablePlugins() {
    // exclude already installed plugin packages whatever the version is
    return getPluginRepositoryCache().getOrUpdatePluginRepository().getPlugins().stream()
      .filter(PluginPackage::hasMicaVersion)
      .filter(pp -> registeredPlugins.stream().noneMatch(rp -> pp.isSameAs(rp.getName())))
      .filter(pp -> runtimeVersionProvider.getVersion().compareTo(pp.getMicaVersion()) >= 0)
      .collect(Collectors.toList());
  }

  /**
   * Perform the plugin installation by retrieving the plugin package from the update site.
   *
   * @param name
   * @param version
   */
  public void installPlugin(String name, String version) {
    String pVersion = version;
    if (Strings.isNullOrEmpty(version)) {
      // no version specified: get the latest
      pVersion = getPluginRepositoryCache().getPluginLatestVersion(name);
    }
    try {
      File tmpDir = Files.createTempDir();
      installPlugin(getPluginRepositoryCache().downloadPlugin(name, pVersion, tmpDir), true);
      FileUtil.delete(tmpDir);
    } catch (IOException e) {
      throw new PluginRepositoryException("Failed to install plugin " + name + ":" + version + " : " + e.getMessage(), e);
    }
  }

  /**
   * Get the installed plugin with the given name.
   *
   * @param name
   * @return
   */
  public PluginResources getInstalledPlugin(String name) {
    Optional<PluginResources> plugin = registeredPlugins.stream().filter(p -> p.getName().equals(name)).findFirst();
    if (!plugin.isPresent()) throw new NoSuchElementException("No such plugin with name: " + name);
    return plugin.get();
  }

  /**
   * Uninstall a plugin.
   *
   * @param name
   */
  public void prepareUninstallPlugin(String name) {
    PluginResources plugin = getInstalledPlugin(name);
    plugin.prepareForUninstall();
  }

  /**
   * Cancel plugin uninstallation before it is effective.
   *
   * @param name
   */
  public void cancelUninstallPlugin(String name) {
    PluginResources plugin = getInstalledPlugin(name);
    plugin.cancelUninstall();
  }

  /**
   * Set the site properties of the installed plugin with the given name.
   *
   * @param name
   * @param properties
   */
  public void setInstalledPluginSiteProperties(String name, String properties) {
    try {
      PluginResources thePlugin = getInstalledPlugin(name);
      thePlugin.writeSiteProperties(properties);
      updateServiceProperties(name, thePlugin.getProperties());
    } catch (IOException e) {
      throw new PluginRepositoryException("Failed to save plugin " + name + " site properties: " + e.getMessage(), e);
    }
  }

  public boolean isInstalledPluginRunning(String name) {
    return getServicePlugin(name).isRunning();
  }

  public void startInstalledPlugin(String name) {
    getServicePlugin(name).start();
  }

  public void stopInstalledPlugin(String name) {
    getServicePlugin(name).stop();
  }

  //
  // Private methods
  //

  private void updateServiceProperties(String name, Properties properties) {
    ServicePlugin servicePlugin = getServicePlugin(name);
    if (servicePlugin != null) {
      servicePlugin.configure(properties);
    }
  }

  ServicePlugin getServicePlugin(String name) {
    Optional<ServicePlugin> service = servicePlugins.stream().filter(s -> name.equals(s.getName())).findFirst();
    if (!service.isPresent()) throw new NoSuchElementException(name);
    return service.get();
  }

  private Collection<ServicePlugin> getServicePlugins(Class clazz) {
    return servicePlugins.stream().filter(s -> clazz.isAssignableFrom(s.getClass())).collect(Collectors.toList());
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (pluginsDir != null) return;
    pluginsDir = new File(PLUGINS_PATH.replace("${MICA_HOME}", System.getProperty("MICA_HOME")));
    if (!pluginsDir.exists() && !pluginsDir.mkdirs()) {
      log.warn("Cannot create directory: {}", pluginsDir.getAbsolutePath());
    }
    archiveDir = new File(pluginsDir, ".archive");
    initPlugins();
  }

  public String getSearchPluginName() {
    return environment.getProperty(MICA_SEARCH_PLUGIN_NAME, DEFAULT_MICA_SEARCH_PLUGIN_NAME);
  }

  /**
   * Initialize plugin resources.
   */
  private void initPlugins() {
    Collection<PluginResources> plugins = getPlugins(true);
    String searchPluginName = getSearchPluginName();

    try {
      String pluginLatestVersion = getPluginRepositoryCache().getPluginLatestVersion(searchPluginName);
      // ensure there is a mica-search plugin installed
      if (plugins.stream().noneMatch(p -> MICA_SEARCH_PLUGIN_TYPE.equals(p.getType()))
        || plugins.stream()
            .filter(plugin -> searchPluginName.equals(plugin.getName()))
            .filter(plugin -> plugin.getVersion().compareTo(new Version(pluginLatestVersion)) >= 0).count() == 0) {
        installPlugin(searchPluginName, null);
        // rescan plugins
        plugins = getPlugins(true);
      }
    } catch (PluginRepositoryException e) {
      log.error("Cannot initialize plugins properly", e);
    }

    boolean micaSearchFound = false; // mica-search plugin is a singleton
    List<PluginResources> filteredPlugins =
      plugins.stream().filter(plugin -> searchPluginName.equals(plugin.getName())
          || MICA_TABLES_PLUGIN_TYPE.equals(plugin.getType())
          || MICA_TAXONOMIES_PLUGIN_TYPE.equals(plugin.getType()))
        .sorted(Comparator.comparing(PluginResources::getVersion))
        .collect(Collectors.toList());

    for (PluginResources plugin : filteredPlugins) {
      if (MICA_SEARCH_PLUGIN_TYPE.equals(plugin.getType()) && !micaSearchFound) {
        initSearchEngineServicePlugin(plugin);
        micaSearchFound = true;
      } else if (MICA_TABLES_PLUGIN_TYPE.equals(plugin.getType())) {
        initStudyTableSourceServicePlugin(plugin);
      } else if (MICA_TAXONOMIES_PLUGIN_TYPE.equals(plugin.getType())) {
        initTaxonomiesProviderServicePlugin(plugin);
      }
    }
  }

  private void initSearchEngineServicePlugin(PluginResources plugin) {
    SearchEngineService service = SearchEngineServiceLoader.get(plugin.getURLClassLoader(false)).iterator().next();
    Properties properties = cleanProperties(plugin.getProperties());
    for (String key : ES_CONFIGURATION) {
      if (environment.containsProperty(key))
        properties.setProperty(key, environment.getProperty(key));
    }
    service.configure(properties);
    service.setConfigurationProvider(configurationProvider);
    service.start();
    servicePlugins.add(service);
  }

  private void initStudyTableSourceServicePlugin(PluginResources plugin) {
    StudyTableSourceServiceLoader.get(plugin.getURLClassLoader(false)).forEach(service -> {
      Properties properties = cleanProperties(plugin.getProperties());
      service.configure(properties);
      service.start();
      servicePlugins.add(service);
    });
  }

  private void initTaxonomiesProviderServicePlugin(PluginResources plugin) {
    TaxonomiesProviderServiceLoader.get(plugin.getURLClassLoader(false)).forEach(service -> {
      Properties properties = cleanProperties(plugin.getProperties());
      service.configure(properties);
      service.start();
      servicePlugins.add(service);
    });
  }

  private Properties cleanProperties(Properties properties) {
    properties.setProperty("MICA_HOME", properties.getProperty("OPAL_HOME"));
    properties.remove("OPAL_HOME");
    return properties;
  }

  private synchronized Collection<PluginResources> getPlugins(boolean extract) {
    Map<String, PluginResources> pluginsMap = Maps.newLinkedHashMap();
    // make sure plugins directory exists
    // read it to enhance classpath
    if (!pluginsDir.exists() || !pluginsDir.isDirectory() || !pluginsDir.canRead()) return pluginsMap.values();
    if (extract) PluginsManagerHelper.preparePlugins(pluginsDir, archiveDir);
    processPlugins(pluginsMap, pluginsDir);
    registeredPlugins = pluginsMap.values();
    return registeredPlugins;
  }

  /**
   * Discover valid and most recent version plugins and archive plugins prepared for uninstallation.
   *
   * @param pluginsMap
   * @param pluginsDir
   */
  private void processPlugins(Map<String, PluginResources> pluginsMap, File pluginsDir) {
    File[] children = pluginsDir.listFiles(pathname -> pathname.isDirectory() && !pathname.getName().startsWith("."));
    if (children == null || children.length == 0) return;
    for (File child : children) {
      PluginResources plugin = new MicaPlugin(child);
      PluginsManagerHelper.processPlugin(pluginsMap, plugin, archiveDir);
    }
  }

  private void installPlugin(File pluginFile, boolean rmAfterInstall) {
    try {
      if (!pluginsDir.exists()) pluginsDir.mkdirs();
      FileUtil.copyFile(pluginFile, pluginsDir);
      if (rmAfterInstall) pluginFile.delete();
    } catch (IOException e) {
      throw new PluginRepositoryException("Plugin installation failed: " + e.getMessage(), e);
    }
  }

  private PluginRepositoryCache getPluginRepositoryCache() {
    if (pluginRepositoryCache == null)
      pluginRepositoryCache = new PluginRepositoryCache(runtimeVersionProvider, getUpdateSite());
    return pluginRepositoryCache;
  }
}
