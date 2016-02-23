package org.obiba.mica.micaConfig.service;

import java.security.Key;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.shiro.codec.CodecSupport;
import org.apache.shiro.codec.Hex;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.util.ByteSource;
import org.obiba.mica.config.taxonomies.DatasetTaxonomy;
import org.obiba.mica.config.taxonomies.NetworkTaxonomy;
import org.obiba.mica.config.taxonomies.StudyTaxonomy;
import org.obiba.mica.config.taxonomies.TaxonomyTaxonomy;
import org.obiba.mica.config.taxonomies.VariableTaxonomy;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.obiba.mica.micaConfig.repository.MicaConfigRepository;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;

@Service
@Validated
@EnableConfigurationProperties({ NetworkTaxonomy.class, StudyTaxonomy.class, DatasetTaxonomy.class, VariableTaxonomy.class, TaxonomyTaxonomy.class })
public class MicaConfigService {

  @Inject
  private NetworkTaxonomy networkTaxonomy;

  @Inject
  private StudyTaxonomy studyTaxonomy;

  @Inject
  private DatasetTaxonomy datasetTaxonomy;

  @Inject
  private VariableTaxonomy variableTaxonomy;

  @Inject
  private TaxonomyTaxonomy taxonomyTaxonomy;

  @Inject
  private MicaConfigRepository micaConfigRepository;

  @Inject
  private EventBus eventBus;

  @Inject
  private Environment env;

  private final AesCipherService cipherService = new AesCipherService();

  @NotNull
  public Taxonomy getNetworkTaxonomy() {
    return networkTaxonomy;
  }

  @NotNull
  public Taxonomy getStudyTaxonomy() {
    return studyTaxonomy;
  }

  @NotNull
  public Taxonomy getDatasetTaxonomy() {
    return datasetTaxonomy;
  }

  @NotNull
  public Taxonomy getVariableTaxonomy() {
    return variableTaxonomy;
  }

  @NotNull
  public Taxonomy getTaxonomyTaxonomy() {
    return taxonomyTaxonomy;
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
