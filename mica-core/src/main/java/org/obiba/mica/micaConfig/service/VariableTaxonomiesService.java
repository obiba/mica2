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


import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.support.YamlResourceReader;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.event.FileDeletedEvent;
import org.obiba.mica.file.event.FileUpdatedEvent;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.micaConfig.event.CacheClearEvent;
import org.obiba.mica.micaConfig.event.VariableTaxonomiesUpdatedEvent;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.spi.search.support.AttributeKey;
import org.obiba.mica.spi.taxonomies.TaxonomiesProviderService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.obiba.opal.core.support.yaml.TaxonomyYaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Access to the variable taxonomies, from the different providers, including opal, local files and plugins.
 */
@Component
public class VariableTaxonomiesService implements EnvironmentAware, InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(VariableTaxonomiesService.class);

  private static final String VARIABLE_TAXONOMIES_PATH = "${MICA_HOME}/conf/taxonomies/variable";

  private Environment environment;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private EventBus eventBus;

  @Inject
  private OpalService opalService;

  @Inject
  private PluginsService pluginsService;

  @Inject
  private FileSystemService fileSystemService;

  @Inject
  private FileStoreService fileStoreService;

  private File variableTaxonomiesDir;

  // Buffer to avoid caching loop
  private Cache<String, Map<String, Taxonomy>> taxonomiesCache = CacheBuilder.newBuilder()
    .maximumSize(1)
    .expireAfterWrite(1, TimeUnit.MINUTES)
    .build();

  @Override
  public void afterPropertiesSet() throws Exception {
    if (variableTaxonomiesDir == null) {
      variableTaxonomiesDir = new File(VARIABLE_TAXONOMIES_PATH.replace("${MICA_HOME}", System.getProperty("MICA_HOME")));
      fileSystemService.mkdirs("/taxonomies/variable");
    }
  }

  @Cacheable(value = "variable-taxonomies", key = "'variable'")
  public synchronized List<Taxonomy> getTaxonomies() {
    Map<String, Taxonomy> taxonomiesMap = null;
    try {
      taxonomiesMap = taxonomiesCache.get("variable-taxonomies", this::getTaxonomiesMap);
    } catch (ExecutionException e) {
      taxonomiesMap = getTaxonomiesMap();
    }
    List<Taxonomy> taxonomyList = Lists.newArrayList(taxonomiesMap.values());
    Collections.sort(taxonomyList, Comparator.comparing(TaxonomyEntity::getName));
    return taxonomyList;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Async
  @Subscribe
  public void onFileUpdated(FileUpdatedEvent event) {
    AttachmentState attachmentState = event.getPersistable();
    if ("/taxonomies/variable".equals(attachmentState.getPath()) && attachmentState.getName().endsWith(".yml")) {
      taxonomiesCache.invalidateAll();
      eventBus.post(new CacheClearEvent("variable-taxonomies"));
    }
  }

  @Async
  @Subscribe
  public void onFileDeleted(FileDeletedEvent event) {
    AttachmentState attachmentState = event.getPersistable();
    if ("/taxonomies/variable".equals(attachmentState.getPath()) && attachmentState.getName().endsWith(".yml")) {
      taxonomiesCache.invalidateAll();
      eventBus.post(new CacheClearEvent("variable-taxonomies"));
    }
  }

  //
  // Private methods
  //

  private Map<String, Taxonomy> getTaxonomiesMap() {
    Map<String, Taxonomy> taxonomies = Maps.newConcurrentMap();
    // init with the ones from opal
    try {
      taxonomies.putAll(opalService.getTaxonomiesInternal());
      if (log.isDebugEnabled())
        taxonomies.keySet().forEach(name -> log.debug("Taxonomy from opal: {}", name));
    } catch (Exception e) {
      // ignore
    }
    // read local files
    if (variableTaxonomiesDir.exists()) {
      File[] yamlFiles = variableTaxonomiesDir.listFiles(file -> !file.isDirectory() && file.getName().endsWith(".yml"));
      if (yamlFiles != null) {
        log.info("Fetching local taxonomies: {}", VARIABLE_TAXONOMIES_PATH);
        for (File yamlFile : yamlFiles) {
          try (FileInputStream in = new FileInputStream(yamlFile.getAbsolutePath())) {
            Taxonomy taxonomy = readTaxonomy(in);
            log.debug("Taxonomy from file: {}", yamlFile.getAbsolutePath());
            // override any duplicated taxonomy
            if (taxonomies.containsKey(taxonomy.getName()))
              log.warn("Taxonomy is duplicated and will be overridden: {}", taxonomy.getName());
            taxonomies.put(taxonomy.getName(), taxonomy);
          } catch (Exception e) {
            log.error("Taxonomy file could not be read: {}", yamlFile.getAbsolutePath(), e);
          }
        }
      }
    }
    // read database files
    for (AttachmentState attachmentState : fileSystemService.findAttachmentStates("^/taxonomies/variable$", false)
      .stream()
      .filter(s -> !RevisionStatus.DELETED.equals(s.getRevisionStatus()))
      .filter(s -> !FileUtils.isDirectory(s))
      .filter(s -> s.getName().endsWith(".yml"))
      .collect(Collectors.toList())) {
      Attachment attachment = attachmentState.getAttachment();
      try (InputStream in = fileStoreService.getFile(attachment.getFileReference())) {
        log.debug("Taxonomy from attachment: {}/{}", attachment.getPath(), attachment.getName());
        Taxonomy taxonomy = readTaxonomy(in);
        // override any duplicated taxonomy
        if (taxonomies.containsKey(taxonomy.getName()))
          log.warn("Taxonomy is duplicated and will be overridden: {}", taxonomy.getName());
        taxonomies.put(taxonomy.getName(), taxonomy);
      } catch (Exception e) {
        log.error("Taxonomy attachment file could not be read: {}/{}", attachmentState.getPath(), attachmentState.getName(), e);
      }
    }
    // get the ones from plugins
    for (TaxonomiesProviderService provider : pluginsService.getTaxonomiesProviderServices().stream()
      .filter(provider -> provider.isFor(TaxonomyTarget.VARIABLE))
      .collect(Collectors.toList())) {
      log.info("Fetching taxonomies from plugin: {}", provider.getName());
      try {
        for (Taxonomy taxonomy : provider.getTaxonomies()) {
          log.debug("Taxonomy from plugin {}: {}", provider.getName(), taxonomy.getName());
          // override any duplicated taxonomy
          if (taxonomies.containsKey(taxonomy.getName()))
            log.warn("Taxonomy is duplicated and will be overridden: {}", taxonomy.getName());
          taxonomies.put(taxonomy.getName(), taxonomy);
        }
      } catch (Exception e) {
        log.warn("Taxonomies retrieval from plugin {} failed", provider.getName(), e);
      }
    }
    // apply mica attributes
    taxonomies.replaceAll((k, v) -> applyAttributes(taxonomies.get(k)));
    eventBus.post(new VariableTaxonomiesUpdatedEvent(taxonomies));
    return taxonomies;
  }

  /**
   * Decorate the variable taxonomies with some Mica specific attributes.
   *
   * @param taxonomy
   * @return
   */
  private Taxonomy applyAttributes(Taxonomy taxonomy) {
    String defaultTermsSortOrder = environment.getProperty("opalTaxonomies.defaultTermsSortOrder");

    // make sure there is a title
    if (taxonomy.getTitle().isEmpty()) {
      Map<String, String> title = Maps.newLinkedHashMap();
      for (String locale : micaConfigService.getLocales()) {
        title.put(locale, taxonomy.getName());
      }
      taxonomy.setTitle(title);
    }

    taxonomy.getVocabularies().forEach(vocabulary -> {
      String field = vocabulary.getAttributeValue("field");
      if (Strings.isNullOrEmpty(field)) {
        vocabulary.addAttribute("field",
          "attributes." + AttributeKey.getMapKey(vocabulary.getName(), taxonomy.getName()) + ".und");
      }
      String alias = vocabulary.getAttributeValue("alias");
      if (Strings.isNullOrEmpty(alias)) {
        vocabulary.addAttribute("alias",
          "attributes-" + AttributeKey.getMapKey(vocabulary.getName(), taxonomy.getName()) + "-und");
      }
      if (!Strings.isNullOrEmpty(defaultTermsSortOrder)) {
        vocabulary.addAttribute("termsSortKey", defaultTermsSortOrder);
      }
    });
    return taxonomy;
  }

  private Taxonomy readTaxonomy(InputStream input) {
    TaxonomyYaml yaml = new TaxonomyYaml();
    return yaml.load(input);
  }
}
