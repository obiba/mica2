package org.obiba.mica.micaConfig;

import java.security.Key;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.shiro.codec.CodecSupport;
import org.apache.shiro.codec.Hex;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.eventbus.EventBus;

@Service
@Validated
public class MicaConfigService {

  @Inject
  private MicaConfigRepository micaConfigRepository;

  @Inject
  private EventBus eventBus;

  private final AesCipherService cipherService = new AesCipherService();

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
        "lastModifiedDate", "secretKey");
    micaConfigRepository.save(savedConfig);
    eventBus.post(new MicaConfigUpdatedEvent(getConfig()));
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
