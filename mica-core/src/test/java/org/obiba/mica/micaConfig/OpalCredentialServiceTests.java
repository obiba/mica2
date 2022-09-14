/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig;

import java.security.KeyStoreException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.obiba.mica.dataset.service.KeyStoreService;
import org.obiba.mica.micaConfig.domain.OpalCredential;
import org.obiba.mica.micaConfig.repository.OpalCredentialRepository;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.OpalCredentialService;
import org.obiba.security.KeyStoreManager;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpalCredentialServiceTests {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private OpalCredentialService opalCredentialService;

  @Mock
  private OpalCredentialRepository opalCredentialRepository;

  @Mock
  private MicaConfigService micaConfigService;

  @Mock
  private KeyStoreService keyStoreService;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetOpalCredential() {
    OpalCredential credential = new OpalCredential("https://opal", AuthType.USERNAME, "test", "encrypted");
    when(opalCredentialRepository.findOne("https://opal")).thenReturn(credential);
    when(micaConfigService.decrypt("encrypted")).thenReturn("password");
    OpalCredential actual = opalCredentialService.getOpalCredential("https://opal");

    assertThat(actual, is(credential));
  }

  @Test
  public void testGetOpalCredentialThrowsException() {
    when(opalCredentialRepository.findOne("https://opal")).thenReturn(null);
    exception.expect(NoSuchOpalCredential.class);

    opalCredentialService.getOpalCredential("https://opal");
  }

  @Test
  public void testCreateUsernamePasswordCredential() {
    when(opalCredentialRepository.findOne("https://opal")).thenReturn(null);
    when(micaConfigService.encrypt("password")).thenReturn("encrypted");

    opalCredentialService.createOrUpdateOpalCredential("https://opal", "test", "password");

    verify(opalCredentialRepository).save(any(OpalCredential.class));
  }

  @Test
  public void testUpdateUsernamePasswordCredential() {
    OpalCredential credential = new OpalCredential("https://opal", AuthType.USERNAME, "test", "encrypted");
    when(opalCredentialRepository.findOne("https://opal")).thenReturn(credential);
    when(micaConfigService.encrypt("password")).thenReturn("encrypted");

    opalCredentialService.createOrUpdateOpalCredential("https://opal", "test", "password");

    verify(opalCredentialRepository).save(any(OpalCredential.class));
  }

  @Test
  public void testDeleteCertificateCredential() throws KeyStoreException {
    OpalCredential credential = new OpalCredential("https://opal", AuthType.CERTIFICATE);
    when(opalCredentialRepository.findOne("https://opal")).thenReturn(credential);
    KeyStoreManager keyStore = mock(KeyStoreManager.class);
    doNothing().when(keyStore).deleteKey("https://opal");
    when(keyStoreService.getKeyStore("opal")).thenReturn(keyStore);

    opalCredentialService.deleteOpalCredential("https://opal");

    verify(opalCredentialRepository).delete(any(OpalCredential.class));
  }
}
