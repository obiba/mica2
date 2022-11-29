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
import org.obiba.mica.spi.dataset.StudyTableSourceService;
import org.obiba.mica.spi.dataset.StudyTableSourceServiceLoader;
import org.obiba.mica.spi.search.ConfigurationProvider;
import org.obiba.mica.spi.search.SearchEngineService;
import org.obiba.mica.spi.search.SearchEngineServiceLoader;
import org.obiba.plugins.PluginRepositoryCache;
import org.obiba.plugins.PluginRepositoryException;
import org.obiba.plugins.PluginResources;
import org.obiba.plugins.PluginsManagerHelper;
import org.obiba.plugins.spi.ServicePlugin;
import org.obiba.runtime.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
public class PluginsService implements EnvironmentAware {

  private static final Logger log = LoggerFactory.getLogger(PluginsService.class);

  private static final String PLUGINS_PATH = "${MICA_HOME}/plugins";

  private static final String MICA_SEARCH_PLUGIN_NAME = "plugins.micaSearchPlugin";

  private static final String DEFAULT_MICA_SEARCH_PLUGIN_NAME = "mica-search-es";

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

  //
  // Private methods
  //

  private Collection<ServicePlugin> getServicePlugins(Class clazz) {
    return servicePlugins.stream().filter(s -> clazz.isAssignableFrom(s.getClass())).collect(Collectors.toList());
  }

  @PostConstruct
  public void init() {
    if (pluginsDir != null) return;
    pluginsDir = new File(PLUGINS_PATH.replace("${MICA_HOME}", System.getProperty("MICA_HOME")));
    if (!pluginsDir.exists() && !pluginsDir.mkdirs()) {
      log.warn("Cannot create directory: {}", pluginsDir.getAbsolutePath());
    }
    archiveDir = new File(pluginsDir, ".archive");
    initPlugins();
  }

  /**
   * Initialize plugin resources.
   */
  private void initPlugins() {
    Collection<PluginResources> plugins = getPlugins(true);
    String pluginName = environment.getProperty(MICA_SEARCH_PLUGIN_NAME, DEFAULT_MICA_SEARCH_PLUGIN_NAME);

    try {
      String pluginLatestVersion = getPluginRepositoryCache().getPluginLatestVersion(pluginName);
      // ensure there is a mica-search plugin installed
      if (plugins.stream().noneMatch(p -> "mica-search".equals(p.getType()))
        || plugins.stream()
            .filter(plugin -> pluginName.equals(plugin.getName()))
            .filter(plugin -> plugin.getVersion().compareTo(new Version(pluginLatestVersion)) >= 0).count() == 0) {
        installPlugin(pluginName, null);
        // rescan plugins
        plugins = getPlugins(true);
      }
    } catch (PluginRepositoryException e) {
      log.error("Cannot initialize plugins properly", e);
    }

    boolean micaSearchFound = false; // mica-search plugin is a singleton
    List<PluginResources> filteredPlugins =
      plugins.stream().filter(plugin -> pluginName.equals(plugin.getName()))
        .sorted(Comparator.comparing(PluginResources::getVersion))
        .collect(Collectors.toList());

    for (PluginResources plugin : filteredPlugins) {
      if ("mica-search".equals(plugin.getType()) && !micaSearchFound) {
        initSearchEngineServicePlugin(plugin);
        micaSearchFound = true;
      }
    }
  }

  private void initSearchEngineServicePlugin(PluginResources plugin) {
    SearchEngineService service = SearchEngineServiceLoader.get(plugin.getURLClassLoader(false)).iterator().next();
    Properties properties = plugin.getProperties();
    for (String key : ES_CONFIGURATION) {
      if (environment.containsProperty(key))
        properties.setProperty(key, environment.getProperty(key));
    }
    service.configure(properties);
    service.setConfigurationProvider(configurationProvider);
    service.start();
    servicePlugins.add(service);
  }

  private void initStudyTableSourceServicePlugins(PluginResources plugin) {
    StudyTableSourceServiceLoader.get(plugin.getURLClassLoader(false)).forEach(service -> {
      service.start();
      servicePlugins.add(service);
    });
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

  private void installPlugin(String name, String version) {
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
      pluginRepositoryCache = new PluginRepositoryCache(runtimeVersionProvider, environment.getProperty("plugins.updateSite", DEFAULT_PLUGINS_UPDATE_SITE));
    return pluginRepositoryCache;
  }
}
