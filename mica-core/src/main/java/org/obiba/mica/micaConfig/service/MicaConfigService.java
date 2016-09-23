/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Iterator;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.apache.shiro.codec.CodecSupport;
import org.apache.shiro.codec.Hex;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.util.ByteSource;
import org.obiba.mica.core.domain.TaxonomyTarget;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.obiba.mica.micaConfig.repository.MicaConfigRepository;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;

@Service
@Validated
public class MicaConfigService {

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
    if(micaConfigRepository.count() == 0) {
      MicaConfig micaConfig = new MicaConfig();
      micaConfig.getLocales().add(MicaConfig.DEFAULT_LOCALE);
      micaConfig.setSecretKey(generateSecretKey());
      micaConfigRepository.save(micaConfig);
      return getConfig();
    }

    return micaConfigRepository.findAll().get(0);
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
      return "https://" + host + ":" + port;
    }
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
    JsonNode original = objectMapper.readTree(translations);

    if (config.hasTranslations() && config.getTranslations().get(locale) != null) {
      JsonNode custom = objectMapper.readTree(config.getTranslations().get(locale));
      return mergeJson(original, custom).toString();
    }

    return original.toString();
  }

  private Resource getTranslationsResource(String locale) {
    return applicationContext.getResource(String.format("classpath:/i18n/%s.json", locale));
  }

  private JsonNode mergeJson(JsonNode mainNode, JsonNode updateNode) {
    Iterator<String> fieldNames = updateNode.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      JsonNode jsonNode = mainNode.get(fieldName);
      if (jsonNode != null && jsonNode.isObject()) {
        mergeJson(jsonNode, updateNode.get(fieldName));
      }
      else {
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
    ByteSource decrypted = cipherService.decrypt(Hex.decode(encrypted), getSecretKey());
    return CodecSupport.toString(decrypted.getBytes());
  }

  private String generateSecretKey() {
    Key key = cipherService.generateNewKey();
    return Hex.encodeToString(key.getEncoded());
  }

  private byte[] getSecretKey() {
    return Hex.decode(getOrCreateMicaConfig().getSecretKey());
  }
}
