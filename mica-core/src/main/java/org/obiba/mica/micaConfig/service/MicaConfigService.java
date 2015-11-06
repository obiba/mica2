package org.obiba.mica.micaConfig.service;

import java.security.Key;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.shiro.codec.CodecSupport;
import org.apache.shiro.codec.Hex;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.util.ByteSource;
import org.obiba.mica.config.AggregationsConfiguration;
import org.obiba.mica.config.StudiesConfiguration;
import org.obiba.mica.micaConfig.domain.AggregationsConfig;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.repository.MicaConfigRepository;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.eventbus.EventBus;

@Service
@Validated
@EnableConfigurationProperties({ AggregationsConfiguration.class, StudiesConfiguration.class })
public class MicaConfigService {

  @Inject
  private AggregationsConfiguration aggregationsConfiguration;

  @Inject
  private StudiesConfiguration studiesConfiguration;

  @Inject
  private MicaConfigRepository micaConfigRepository;

  @Inject
  private EventBus eventBus;

  @Inject
  private Environment env;

  private final AesCipherService cipherService = new AesCipherService();

  @NotNull
  public AggregationsConfig getAggregationsConfig() {
    AggregationsConfig aggregationsConfig = getConfig().getAggregations();

    if(aggregationsConfig == null) {
      aggregationsConfig = getDefaultAggregationsConfig();
    }

    return aggregationsConfig;
  }

  @NotNull
  public StudiesConfiguration getStudiesConfiguration() {
    return studiesConfiguration;
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
    BeanUtils.copyProperties(micaConfig, savedConfig, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
      "lastModifiedDate", "secretKey", "micaVersion");
    if(micaConfig.getMicaVersion() != null) savedConfig.setMicaVersion(micaConfig.getMicaVersion());
    micaConfigRepository.save(savedConfig);
    eventBus.post(new MicaConfigUpdatedEvent(getConfig()));
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

  private AggregationsConfig getDefaultAggregationsConfig() {
    AggregationsConfig aggregationsConfig = new AggregationsConfig();
    aggregationsConfig.setNetworkAggregations(aggregationsConfiguration.getNetwork());
    aggregationsConfig.setStudyAggregations(aggregationsConfiguration.getStudy());
    aggregationsConfig.setDatasetAggregations(aggregationsConfiguration.getDataset());
    aggregationsConfig.setVariableAggregations(aggregationsConfiguration.getVariable());

    return aggregationsConfig;
  }

}
