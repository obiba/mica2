/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config.security;

import java.io.IOException;

import jakarta.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class SecretKeyCallbackHandler implements CallbackHandler {

  @Inject
  private MicaConfigService micaConfigService;

  @Override
  public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
    if(callbacks == null || callbacks.length < 1) return;
    Callback callback = callbacks[0];
    if(callback instanceof PasswordCallback) {
      ((PasswordCallback) callback).setPassword(getPassword());
      return;
    }
    throw new UnsupportedCallbackException(callback);
  }

  private char[] getPassword() {
    return micaConfigService.getConfig().getSecretKey().toCharArray();
  }
}
