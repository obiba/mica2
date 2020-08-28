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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.apache.commons.io.FileUtils;
import org.apache.shiro.codec.CodecSupport;
import org.apache.shiro.codec.Hex;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.crypto.CryptoException;
import org.apache.shiro.util.ByteSource;
import org.obiba.mica.micaConfig.MissingConfigurationException;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.obiba.mica.micaConfig.repository.MicaConfigRepository;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@Validated
public class MicaConfigService {

  private static final Logger logger = LoggerFactory.getLogger(MicaConfigService.class);

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private TaxonomyConfigService taxonomyConfigService;

  @Inject
  private MicaConfigRepository micaConfigRepository;

  @Inject
  private EventBus eventBus;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private Environment env;

  private final AesCipherService cipherService = new AesCipherService();

  public List<String> getLocales() {
    return getConfig().getLocalesAsString();
  }

  public List<String> getRoles() {
    return getConfig().getRoles();
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public Taxonomy getTaxonomy(TaxonomyTarget target) {
    return taxonomyConfigService.findByTarget(target);
  }

  @NotNull
  Taxonomy getNetworkTaxonomy() {
    return taxonomyConfigService.findByTarget(TaxonomyTarget.NETWORK);
  }

  @NotNull
  Taxonomy getStudyTaxonomy() {
    return taxonomyConfigService.findByTarget(TaxonomyTarget.STUDY);
  }

  @NotNull
  Taxonomy getDatasetTaxonomy() {
    return taxonomyConfigService.findByTarget(TaxonomyTarget.DATASET);
  }

  @NotNull
  Taxonomy getVariableTaxonomy() {
    return taxonomyConfigService.findByTarget(TaxonomyTarget.VARIABLE);
  }

  @NotNull
  Taxonomy getTaxonomyTaxonomy() {
    return taxonomyConfigService.findByTarget(TaxonomyTarget.TAXONOMY);
  }

  @Cacheable(value = "micaConfig", key = "#root.methodName")
  public MicaConfig getConfig() {
    return getOrCreateMicaConfig();
  }

  private MicaConfig getOrCreateMicaConfig() {
    MicaConfig config;
    if(micaConfigRepository.count() == 0) {
      MicaConfig micaConfig = new MicaConfig();
      micaConfig.getLocales().add(MicaConfig.DEFAULT_LOCALE);
      micaConfig.setSecretKey(generateSecretKey());
      micaConfigRepository.save(micaConfig);
    }

    config = micaConfigRepository.findAll().get(0);
    config.setContextPath(getContextPath());
    return config;
  }

  @CacheEvict(value = "micaConfig", allEntries = true)
  public void save(@NotNull @Valid MicaConfig micaConfig) {
    MicaConfig savedConfig = getOrCreateMicaConfig();
    ArrayList<String> removedRoles = Lists
      .newArrayList(Sets.difference(Sets.newHashSet(savedConfig.getRoles()), Sets.newHashSet(micaConfig.getRoles())));

    BeanUtils.copyProperties(micaConfig, savedConfig, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
      "lastModifiedDate", "secretKey", "micaVersion");

    if(micaConfig.getMicaVersion() != null) savedConfig.setMicaVersion(micaConfig.getMicaVersion());

    micaConfigRepository.save(savedConfig);
    eventBus.post(new MicaConfigUpdatedEvent(getConfig(), removedRoles));
  }

  /**
   * Get the http server context path, if configured.
   * @return
   */
  public String getContextPath() {
    String contextPath = env.getProperty("server.context-path", "");
    return Strings.isNullOrEmpty(contextPath) ? env.getProperty("server.servlet.context-path", "") : contextPath;
  }

  /**
   * Get the public url, statically defined if not part of the {@link org.obiba.mica.micaConfig.domain.MicaConfig}.
   *
   * @return
   */
  public String getPublicUrl() {
    MicaConfig config = getConfig();

    if(config.hasPublicUrl()) {
      return config.getPublicUrl();
    } else {
      String host = env.getProperty("server.address");
      String port = env.getProperty("https.port");
      return "https://" + host + ":" + port + getContextPath();
    }
  }

  public String getPortalUrl() {
    MicaConfig config = getConfig();
    if (config.getPortalUrl() != null)
      return config.getPortalUrl();
    else
      throw new MissingConfigurationException("Empty portal url. Impossible to generate portal link.");
  }

  public String getTranslations(@NotNull String locale, boolean _default) throws IOException {
    File translations;

    try {
      translations = getTranslationsResource(locale).getFile();
    } catch (IOException e) {
      locale = "en";
      translations = getTranslationsResource(locale).getFile();
    }

    if(_default) {
      return FileUtils.readFileToString(translations, "utf-8");
    }

    MicaConfig config = getOrCreateMicaConfig();
    JsonNode builtTranslations = objectMapper.readTree(translations);

    builtTranslations = addTaxonomies(builtTranslations, locale);

    if (config.hasTranslations() && config.getTranslations().get(locale) != null) {
      JsonNode custom = objectMapper.readTree(config.getTranslations().get(locale));
      return mergeJson(builtTranslations, custom).toString();
    }

    return builtTranslations.toString();
  }

  private JsonNode addTaxonomies(JsonNode translations, String locale) {
    translations = addTaxonomy(TaxonomyTarget.STUDY, "study_taxonomy", translations, locale);
    translations = addTaxonomy(TaxonomyTarget.NETWORK, "network_taxonomy", translations, locale);
    translations = addTaxonomy(TaxonomyTarget.DATASET, "dataset_taxonomy", translations, locale);
    return addTaxonomy(TaxonomyTarget.VARIABLE, "variable_taxonomy", translations, locale);
  }

  private JsonNode addTaxonomy(TaxonomyTarget taxonomyTarget, String taxonomyKey, JsonNode original, String locale) {

    JsonNode taxonomyNode = getTaxonomyNode(taxonomyTarget, locale);

    ObjectNode containerNode = new ObjectNode(JsonNodeFactory.instance);
    containerNode.set(taxonomyKey, taxonomyNode);

    return mergeJson(original, containerNode);
  }

  private JsonNode getTaxonomyNode(TaxonomyTarget taxonomyTarget, String locale) {

    Taxonomy taxonomy = taxonomyConfigService.findByTarget(taxonomyTarget);

    ObjectNode vocabularies = new ObjectNode(JsonNodeFactory.instance);
    for (Vocabulary vocabulary : taxonomy.getVocabularies()) {
      ObjectNode vocabularyNode = createObjectNode(vocabulary, locale);
      vocabularyNode = addTerms(vocabularyNode, vocabulary.getTerms(), locale);
      vocabularies.set(vocabulary.getName(),vocabularyNode);
    }

    ObjectNode taxonomyNode = createObjectNode(taxonomy, locale);
    taxonomyNode.set("vocabulary", vocabularies);

    return taxonomyNode;
  }

  private ObjectNode addTerms(ObjectNode parentNode, List<Term> terms, String locale) {
    if (!isEmpty(terms)) {
      ObjectNode termsArrayNode = new ObjectNode(JsonNodeFactory.instance);
      for (Term term : terms) {
        ObjectNode termNode = createObjectNode(term, locale);
        termNode = addTerms(termNode, term.getTerms(), locale);
        termsArrayNode.set(term.getName(), termNode);
      }
      parentNode.set("term", termsArrayNode);
    }
    return parentNode;
  }

  private ObjectNode createObjectNode(TaxonomyEntity taxonomy, String locale) {
    ObjectNode taxonomyNode = new ObjectNode(JsonNodeFactory.instance);
    putFieldInNodeIfExists(taxonomyNode, "title", taxonomy.getTitle().get(locale));
    putFieldInNodeIfExists(taxonomyNode, "description", taxonomy.getDescription().get(locale));
    return taxonomyNode;
  }

  private void putFieldInNodeIfExists(ObjectNode node, String key, String value) {
    if (value != null)
      node.put(key, value);
  }

  private Resource getTranslationsResource(String locale) {
    return applicationContext.getResource(String.format("classpath:/i18n/%s.json", locale));
  }

  public JsonNode mergeJson(JsonNode mainNode, JsonNode updateNode) {
    if (updateNode == null) return mainNode;
    Iterator<String> fieldNames = updateNode.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      JsonNode jsonNode = mainNode.get(fieldName);
      if (jsonNode != null && jsonNode.isObject()) {
        mergeJson(jsonNode, updateNode.get(fieldName));
      } else {
        if (mainNode instanceof ObjectNode) {
          JsonNode value = updateNode.get(fieldName);
          ((ObjectNode) mainNode).replace(fieldName, value);
        }
      }
    }

    return mainNode;
  }

  public String encrypt(String plain) {
    ByteSource encrypted = cipherService.encrypt(CodecSupport.toBytes(plain), getSecretKey());
    return encrypted.toHex();
  }

  public String decrypt(String encrypted) {
    try {
      ByteSource decrypted = cipherService.decrypt(Hex.decode(encrypted), getSecretKey());
      return CodecSupport.toString(decrypted.getBytes());
    } catch (CryptoException e) {
      logger.warn(String.format("Someone tried to use an invalid key [%s]", encrypted));
      throw new IllegalArgumentException("Given key is invalid", e);
    }
  }

  private String generateSecretKey() {
    Key key = cipherService.generateNewKey();
    return Hex.encodeToString(key.getEncoded());
  }

  private byte[] getSecretKey() {
    return Hex.decode(getOrCreateMicaConfig().getSecretKey());
  }
}
