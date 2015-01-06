/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.service;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.security.auth.callback.CallbackHandler;
import javax.validation.constraints.NotNull;

import org.obiba.security.KeyStoreManager;
import org.obiba.security.KeyStoreRepository;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class KeyStoreService {

  private static final String SYSTEM_KEY_STORE = "system";

  private static final String PATH_KEYSTORE = "${MICA_HOME}/data/keystores";

  @Inject
  private CallbackHandler callbackHandler;

  private File keystoresRoot;

  private final KeyStoreRepository keyStoreRepository = new KeyStoreRepository();

  @PostConstruct
  public void init() {
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
}
