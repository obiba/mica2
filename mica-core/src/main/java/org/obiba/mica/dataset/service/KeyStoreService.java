/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.service;

import com.google.common.base.Strings;
import org.bouncycastle.openssl.PEMWriter;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.security.KeyStoreManager;
import org.obiba.security.KeyStoreRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.security.auth.callback.CallbackHandler;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 *
 */
@Service
public class KeyStoreService implements InitializingBean {

  public static final String SYSTEM_KEY_STORE = "system";

  private static final String PATH_KEYSTORE = "${MICA_HOME}/data/keystores";

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private CallbackHandler callbackHandler;

  private File keystoresRoot;

  private final KeyStoreRepository keyStoreRepository = new KeyStoreRepository();

  @Override
  public void afterPropertiesSet() throws Exception {
    if(keystoresRoot == null) {
      keystoresRoot = new File(PATH_KEYSTORE.replace("${MICA_HOME}", System.getProperty("MICA_HOME")));
    }
    keyStoreRepository.setKeyStoresDirectory(keystoresRoot);
    keyStoreRepository.setCallbackHandler(callbackHandler);
  }

  @NotNull
  public KeyStoreManager getSystemKeyStore() {
    return keyStoreRepository.getOrCreateKeyStore(SYSTEM_KEY_STORE);
  }

  public void saveKeyStore(@NotNull KeyStoreManager keyStore) {
    keyStoreRepository.saveKeyStore(keyStore);
  }

  @NotNull
  public KeyStoreManager getKeyStore(@NotNull String name) {
    return keyStoreRepository.getOrCreateKeyStore(name);
  }

  @NotNull
  public String getPEMCertificate(@NotNull String name, String alias) throws KeyStoreException, IOException {
    Certificate[] certificates = getKeyStore(name).getKeyStore().getCertificateChain(alias);
    if (certificates == null || certificates.length == 0) throw new IllegalArgumentException("Cannot find certificate for alias: " + alias);

    StringWriter writer = new StringWriter();
    PEMWriter pemWriter = new PEMWriter(writer);
    for (Certificate certificate : certificates) {
      pemWriter.writeObject(certificate);
    }
    pemWriter.flush();
    return writer.getBuffer().toString();
  }

  public void createOrUpdateCertificate(String name, String alias, String publicCertificate) {
    KeyStoreManager ksm = getKeyStore(name);
    ksm.importCertificate(alias, new ByteArrayInputStream(publicCertificate.getBytes()));
    saveKeyStore(ksm);
  }

  public void createOrUpdateCertificate(String name, String alias, String algo, int size, String cn, String ou,
    String o, String locality, String state, String country) {
    KeyStoreManager ksm = getKeyStore(name);
    ksm.createOrUpdateKey(alias, algo, size, getCertificateInfo(cn, ou, o, locality, state, country));
    saveKeyStore(ksm);
  }

  public void createOrUpdateCertificate(String name, String alias, String privateKey, String cn, String ou, String o, String locality,
    String state, String country) {
    KeyStoreManager ksm = getKeyStore(name);
    ksm.importKey(alias, new ByteArrayInputStream(privateKey.getBytes()),
        getCertificateInfo(cn, ou, o, locality, state, country));
    saveKeyStore(ksm);
  }

  public void createOrUpdateCertificate(String name, String alias, String privateKey, String publicCertificate) {
    KeyStoreManager ksm = getKeyStore(name);
    ksm.importKey(alias, new ByteArrayInputStream(privateKey.getBytes()),
        new ByteArrayInputStream(publicCertificate.getBytes()));
    saveKeyStore(ksm);
  }

  public void deleteKeyPair(String name, String alias) {
    KeyStoreManager ksm = getKeyStore(name);

    if (ksm.hasKeyPair(alias)) {
      ksm.deleteKey(alias);
      saveKeyStore(ksm);
    }
  }

  private String getCertificateInfo(String cn, String ou, String o, String locality, String state, String country) {
    return validateNameAndOrganizationInfo(cn, ou, o) + ", L=" + ofNullable(locality).orElse("") + ", ST=" +
      ofNullable(state).orElse("") + ", C=" + ofNullable(country).orElse("");
  }

  private String validateNameAndOrganizationInfo(String cn, String ou, String o) {
    Optional<String> hostname = Optional.empty();

    if(Strings.isNullOrEmpty(cn) || Strings.isNullOrEmpty(o)) {
      try {
        hostname = Optional.of(new URL(micaConfigService.getConfig().getPublicUrl()).getHost());
      } catch(MalformedURLException e) {
        //ignore
      }
    }

    return String.format("CN=%s, OU=%s, O=%s", Strings.isNullOrEmpty(cn) ? hostname.orElse("") : cn,
      Strings.isNullOrEmpty(ou) ? "mica" : ou, ofNullable(o).orElse(""));
  }
}
