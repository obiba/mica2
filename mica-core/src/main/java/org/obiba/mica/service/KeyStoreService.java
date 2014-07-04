/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.validation.constraints.NotNull;

import org.obiba.crypt.CacheablePasswordCallback;
import org.obiba.crypt.CachingCallbackHandler;
import org.obiba.crypt.KeyProviderSecurityException;
import org.obiba.opal.core.security.OpalKeyStore;
import org.obiba.security.KeyStoreManager;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class KeyStoreService {

  private static final String SYSTEM_KEY_STORE = "system";

  private static final String PATH_KEYSTORE = "${MICA_SERVER_HOME}/data/keystores";

  @Inject
  private CallbackHandler callbackHandler;

  private File keystoresRoot;

  @PostConstruct
  public void init() {
    if(keystoresRoot == null) {
      keystoresRoot = new File(PATH_KEYSTORE.replace("${MICA_SERVER_HOME}", System.getProperty("MICA_SERVER_HOME")));
      keystoresRoot.mkdirs();
    }
  }

  @NotNull
  public KeyStoreManager getSystemKeyStore() {
    return getOrCreateKeyStore(SYSTEM_KEY_STORE);
  }

  public void saveKeyStore(@NotNull KeyStoreManager keyStore) {
    try {
      Path keyStorePath = getKeystoreFile(keyStore.getName()).toPath();
      Files.write(keyStorePath, getKeyStoreByteArray(keyStore), StandardOpenOption.CREATE,
          StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  KeyStoreManager getOrCreateKeyStore(@NotNull String name) {
    KeyStoreManager keyStore = getKeyStore(name);
    if(keyStore == null) {
      keyStore = KeyStoreManager.Builder.newStore().name(name).passwordPrompt(callbackHandler).build();
      saveKeyStore(keyStore);
    }
    return keyStore;
  }

  private KeyStoreManager getKeyStore(@NotNull String name) {
    File keyStoreFile = getKeystoreFile(name);
    return keyStoreFile.exists() ? loadKeyStore(name, getKeystoreFile(name)) : null;
  }

  private File getKeystoreFile(@NotNull String name) {
    return new File(keystoresRoot, name);
  }

  private KeyStoreManager loadKeyStore(@NotNull String name, @NotNull File keyStoreFile) {
    CacheablePasswordCallback passwordCallback = CacheablePasswordCallback.Builder.newCallback().key(name)
        .prompt(getPasswordFor(name)).build();

    KeyStoreManager keyStore = null;
    try {
      keyStore = new KeyStoreManager(name, loadKeyStore(Files.readAllBytes(keyStoreFile.toPath()), passwordCallback));
      keyStore.setCallbackHandler(callbackHandler);
      OpalKeyStore.loadBouncyCastle();
    } catch(GeneralSecurityException | UnsupportedCallbackException ex) {
      throw new RuntimeException(ex);
    } catch(IOException ex) {
      clearPasswordCache(callbackHandler, name);
      translateAndRethrowKeyStoreIOException(ex);
    }
    return keyStore;
  }

  private KeyStore loadKeyStore(byte[] keyStoreBytes, CacheablePasswordCallback passwordCallback)
      throws GeneralSecurityException, UnsupportedCallbackException, IOException {
    KeyStore ks = KeyStore.getInstance("JCEKS");
    ks.load(new ByteArrayInputStream(keyStoreBytes), getKeyPassword(passwordCallback));
    return ks;
  }

  private char[] getKeyPassword(CacheablePasswordCallback passwordCallback)
      throws UnsupportedCallbackException, IOException {
    callbackHandler.handle(new CacheablePasswordCallback[] { passwordCallback });
    return passwordCallback.getPassword();
  }

  private byte[] getKeyStoreByteArray(KeyStoreManager keyStore) {
    String name = keyStore.getName();

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      CacheablePasswordCallback passwordCallback = CacheablePasswordCallback.Builder.newCallback().key(name)
          .prompt(getPasswordFor(name)).build();
      keyStore.getKeyStore().store(outputStream, getKeyPassword(passwordCallback));
    } catch(KeyStoreException e) {
      clearPasswordCache(callbackHandler, keyStore.getName());
      throw new KeyProviderSecurityException("Wrong keystore password or keystore was tampered with");
    } catch(GeneralSecurityException | UnsupportedCallbackException e) {
      throw new RuntimeException(e);
    } catch(IOException ex) {
      clearPasswordCache(callbackHandler, name);
      translateAndRethrowKeyStoreIOException(ex);
    }
    return outputStream.toByteArray();
  }

  /**
   * Returns "Password for 'name':  ".
   */
  private String getPasswordFor(String name) {
    return KeyStoreManager.PASSWORD_FOR + " '" + name + "':  ";
  }

  private static void clearPasswordCache(CallbackHandler callbackHandler, String passwordKey) {
    if(callbackHandler instanceof CachingCallbackHandler) {
      ((CachingCallbackHandler) callbackHandler).clearPasswordCache(passwordKey);
    }
  }

  private static void translateAndRethrowKeyStoreIOException(IOException ex) {
    if(ex.getCause() != null && ex.getCause() instanceof UnrecoverableKeyException) {
      throw new KeyProviderSecurityException("Wrong keystore password");
    }
    throw new RuntimeException(ex);
  }
}
