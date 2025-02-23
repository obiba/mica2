/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config.ssl;

import jakarta.inject.Inject;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;

import org.obiba.mica.dataset.service.KeyStoreService;
import org.obiba.security.KeyStoreManager;
import org.obiba.ssl.SslContextFactory;
import org.obiba.ssl.X509ExtendedKeyManagerImpl;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Build a {@code SSLContext} based on the system keystore.
 */
@Component
public class SslContextFactoryImpl implements SslContextFactory, EnvironmentAware {

  private Environment environment;

  @Inject
  private KeyStoreService keyStoreService;

  @Override
  public SSLContext createSslContext() {
    KeyStoreManager keystore = prepareServerKeystore();
    try {
      SSLContext ctx = SSLContext.getInstance("TLSv1.2");
      ctx.init(new KeyManager[] { new X509ExtendedKeyManagerImpl(keystore) }, null, null);
      return ctx;
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Prepares the keystore for serving HTTPs requests. This method will create the keystore if it does not exist
   * and generate a self-signed certificate. If the keystore already exists, it is not modified in any way.
   *
   * @return a prepared keystore
   */
  private KeyStoreManager prepareServerKeystore() {
    KeyStoreManager keystore = keyStoreService.getSystemKeyStore();
    if(!keystore.aliasExists(X509ExtendedKeyManagerImpl.HTTPS_ALIAS)) {
      keystore.createOrUpdateKey(X509ExtendedKeyManagerImpl.HTTPS_ALIAS, "RSA", 2048, generateCertificateInfo());
      keyStoreService.saveKeyStore(keystore);
    }
    return keystore;
  }

  private String generateCertificateInfo() {
    String hostname = environment.getProperty("server.address", "localhost");
    return "CN=" + hostname + ", OU=Mica, O=" + hostname + ", L=, ST=, C=";
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }
}
