/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.obiba.mica.core.support.YamlResourceReader;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.spi.taxonomies.TaxonomiesProviderService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Access to the variable taxonomies, from the different providers, including opal, local files and plugins.
 */
@Component
public class VariableTaxonomiesService {

  private static final Logger log = LoggerFactory.getLogger(VariableTaxonomiesService.class);

  private static final String VARIABLE_TAXONOMIES_PATH = "${MICA_HOME}/conf/taxonomies/variable";

  @Inject
  private OpalService opalService;

  @Inject
  private PluginsService pluginsService;

  private File variableTaxonomiesDir;

  @PostConstruct
  public void init() {
    if (variableTaxonomiesDir == null) {
      variableTaxonomiesDir = new File(VARIABLE_TAXONOMIES_PATH.replace("${MICA_HOME}", System.getProperty("MICA_HOME")));
    }
  }

  @Cacheable(value = "variable-taxonomies", key = "'variable'")
  public List<Taxonomy> getTaxonomies() {
    List<Taxonomy> taxonomyList = Lists.newArrayList(getTaxonomiesMap().values());
    Collections.sort(taxonomyList, Comparator.comparing(TaxonomyEntity::getName));
    return taxonomyList;
  }

  //
  // Private methods
  //

  private Map<String, Taxonomy> getTaxonomiesMap() {
    Map<String, Taxonomy> taxonomies = Maps.newHashMap();
    // init with the ones from opal
    try {
      taxonomies.putAll(opalService.getTaxonomiesInternal());
    } catch (Exception e) {
      // ignore
    }
    // read local files
    if (variableTaxonomiesDir.exists()) {
      File[] yamlFiles = variableTaxonomiesDir.listFiles(file -> !file.isDirectory() && file.getName().endsWith(".yml"));
      if (yamlFiles != null) {
        log.info("Fetching local taxonomies");
        for (File yamlFile : yamlFiles) {
          try {
            Taxonomy taxonomy = YamlResourceReader.readFile(yamlFile.getAbsolutePath(), Taxonomy.class);
            taxonomies.put(taxonomy.getName(), taxonomy);
          } catch (Exception e) {
            log.error("Taxonomy file could not be read: {}", yamlFile.getAbsolutePath(), e);
          }
        }
      }
    }
    // get the ones from plugins
    for (TaxonomiesProviderService provider : pluginsService.getTaxonomiesProviderServices().stream()
      .filter(provider -> provider.isFor(TaxonomyTarget.VARIABLE))
      .collect(Collectors.toList())) {
      log.info("Fetching taxonomies from plugin: {}", provider.getName());
      try {
        for (Taxonomy taxonomy : provider.getTaxonomies()) {
          // override any duplicated taxonomy
          if (taxonomies.containsKey(taxonomy.getName()))
            log.warn("Taxonomy with name {} is duplicated ({} plugin)", provider.getName(), taxonomy.getName());
          taxonomies.put(taxonomy.getName(), taxonomy);
        }
      } catch (Exception e) {
        log.warn("Taxonomies retrieval from plugin {} failed", provider.getName(), e);
      }
    }
    return taxonomies;
  }

}
