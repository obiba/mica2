package org.obiba.mica.micaConfig.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.bouncycastle.openssl.PEMWriter;
import org.obiba.mica.dataset.service.KeyStoreService;
import org.obiba.mica.micaConfig.AuthType;
import org.obiba.mica.micaConfig.NoSuchOpalCredential;
import org.obiba.mica.micaConfig.domain.OpalCredential;
import org.obiba.mica.micaConfig.repository.OpalCredentialRepository;
import org.obiba.security.KeyStoreManager;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static java.util.stream.Collectors.toList;

@Service
@Validated
public class OpalCredentialService {

  @Inject
  private OpalCredentialRepository repository;

  @Inject
  private KeyStoreService keyStoreService;

  @Inject
  private MicaConfigService micaConfigService;

  public boolean hasOpalCredential(String id) {
    return repository.findOne(id) != null;
  }

  @NotNull
  public OpalCredential getOpalCredential(@NotNull String id) throws NoSuchOpalCredential {
    OpalCredential opalCredential = Optional.ofNullable(repository.findOne(id)).orElseThrow(NoSuchOpalCredential::new);

    if(opalCredential.getAuthType() == AuthType.USERNAME)
      opalCredential.setPassword(micaConfigService.decrypt(opalCredential.getPassword()));

    return opalCredential;
  }

  public Optional<OpalCredential> findOpalCredentialById(String id) {
    OpalCredential opalCredential = repository.findOne(id);

    if(opalCredential != null &&  opalCredential.getAuthType() == AuthType.USERNAME)
      opalCredential.setPassword(micaConfigService.decrypt(opalCredential.getPassword()));

    return Optional.ofNullable(opalCredential);
  }

  public List<OpalCredential> findAllOpalCredentials() {
    return repository.findAll().stream().map(c -> {
      if (c.getAuthType() == AuthType.USERNAME)
        c.setPassword(micaConfigService.decrypt(c.getPassword()));

      return c;
    }).collect(toList());
  }

  public void createOrUpdateOpalCredential(String opalUrl, String username, String password) {
    OpalCredential credential = Optional.ofNullable(repository.findOne(opalUrl))
      .map(c -> {
        if(c.getAuthType() == AuthType.CERTIFICATE) deleteKeyPair(opalUrl);
        c.setAuthType(AuthType.USERNAME);
        c.setUsername(username);
        c.setPassword(micaConfigService.encrypt(password));

        return c;
      })
      .orElse(new OpalCredential(opalUrl, AuthType.USERNAME, username, micaConfigService.encrypt(password)));

    repository.save(credential);
  }

  public void createOrUpdateOpalCertificateCredential(String opalUrl, String publicCertificate) {
    saveOrUpdateOpalCertificateCredential(opalUrl,
      ksm -> ksm.importCertificate(opalUrl, new ByteArrayInputStream(publicCertificate.getBytes())));
  }

  public void createOrUpdateOpalCertificateCredential(String opalUrl, String algo, int size, String cn, String ou, String o,
    String locality, String state, String country) {
    saveOrUpdateOpalCertificateCredential(opalUrl,
      ksm -> ksm.createOrUpdateKey(opalUrl, algo, size, getCertificateInfo(cn, ou, o, locality, state, country)));
  }

  public void createOrUpdateOpalCertificateCredential(String opalUrl, String privateKey, String cn, String ou, String o, String locality,
    String state, String country) {
    saveOrUpdateOpalCertificateCredential(opalUrl,
      ksm -> ksm.importKey(opalUrl, new ByteArrayInputStream(privateKey.getBytes()),
        getCertificateInfo(cn, ou, o, locality, state, country)));
  }

  public void createOrUpdateOpalCertificateCredential(String opalUrl, String privateKey, String publicCertificate) {
    saveOrUpdateOpalCertificateCredential(opalUrl,
      ksm -> ksm.importKey(opalUrl, new ByteArrayInputStream(privateKey.getBytes()),
        new ByteArrayInputStream(publicCertificate.getBytes())));
  }

  public void deleteOpalCredential(String opalUrl) {
    OpalCredential credential = repository.findOne(opalUrl);

    if(credential == null) return;

    repository.delete(credential);

    if(credential.getAuthType() == AuthType.CERTIFICATE) {
      deleteKeyPair(opalUrl);
    }
  }

  private void deleteKeyPair(String alias) {
    KeyStoreManager ksm = keyStoreService.getKeyStore(OpalService.OPAL_KEYSTORE);

    if (ksm.hasKeyPair(alias)) {
      ksm.deleteKey(alias);
      keyStoreService.saveKeyStore(ksm);
    }
  }

  public String getCertificate(String opalUrl) {
    try {
      return getPEMCertificate(keyStoreService.getKeyStore(OpalService.OPAL_KEYSTORE), opalUrl);
    } catch(KeyStoreException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void saveOrUpdateOpalCertificateCredential(String opalUrl, Consumer<KeyStoreManager> keyStoreStrategy) {
    OpalCredential credential = Optional.ofNullable(repository.findOne(opalUrl))
      .map(c -> {
        c.setAuthType(AuthType.CERTIFICATE);
        c.setUsername(null);
        c.setPassword(null);

        return c;
      })
      .orElse(new OpalCredential(opalUrl, AuthType.CERTIFICATE));

    repository.save(credential);

    KeyStoreManager ksm = keyStoreService.getKeyStore(OpalService.OPAL_KEYSTORE);
    keyStoreStrategy.accept(ksm);

    keyStoreService.saveKeyStore(ksm);
  }

  private String getCertificateInfo(String cn, String ou, String o, String locality, String state, String country) {
    return validateNameAndOrganizationInfo(cn, ou, o) + ", L=" + locality + ", ST=" + state + ", C=" + country;
  }

  private String validateNameAndOrganizationInfo(String cn, String ou, String o) {
    Optional<String> hostname = Optional.empty();

    if(cn.isEmpty() || o.isEmpty()) {
      try {
        hostname = Optional.of(new URL(micaConfigService.getConfig().getPublicUrl()).getHost());
      } catch(MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    return String.format("CN=%s, OU=%s, O=%s", cn.isEmpty() ? hostname.get() : cn, ou.isEmpty() ? "opal" : ou,
      o.isEmpty() ? hostname.get() : o);
  }

  private String getPEMCertificate(KeyStoreManager keystore, String alias) throws KeyStoreException, IOException {
    Certificate certificate = Optional.ofNullable(keystore.getKeyStore().getCertificate(alias))
      .orElseThrow(() -> new IllegalArgumentException("Cannot find certificate for alias: " + alias));

    StringWriter writer = new StringWriter();
    PEMWriter pemWriter = new PEMWriter(writer);
    pemWriter.writeObject(certificate);
    pemWriter.flush();

    return writer.getBuffer().toString();
  }
}
