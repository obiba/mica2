/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.ini.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.lang.util.Factory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.AbstractShiroTest;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.security.service.SubjectAclService;

@ExtendWith(MockitoExtension.class)
public class MicaConfigDtosTest extends AbstractShiroTest {

  @InjectMocks
  private MicaConfigDtos dtos;

  @Mock
  private LocalizedStringDtos localizedStringDtos;

  @Mock
  private AttachmentDtos attachmentDtos;

  @Mock
  private SubjectAclService subjectAclService;

  @BeforeAll
  public static void beforeClass() {
    Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:test.shiro.ini");
    setSecurityManager(factory.getInstance());
  }

  @BeforeEach
  public void setup() {
    Subject subjectUnderTest = new Subject.Builder(getSecurityManager()).buildSubject();

    UsernamePasswordToken token = new UsernamePasswordToken("root", "secret");
    subjectUnderTest.login(token);

    setSubject(subjectUnderTest);
  }

  @Test
  public void test_default_values() {
    MicaConfig config = new MicaConfig();

    Mica.MicaConfigDto dto = dtos.asDto(config);
    assertThat(dto).isNotNull();

    MicaConfig fromDto = dtos.fromDto(dto);
    assertThat(fromDto).isEqualToIgnoringGivenFields(config, "createdDate");
  }

  @Test
  public void test_with_values() {
    MicaConfig config = new MicaConfig();
    config.setName("Test");
    config.setPublicUrl("http://localhost/mica-test");
    config.setDefaultCharacterSet("utf-8");
    config.getLocales().add(Locale.CHINESE);
    config.getLocales().add(Locale.GERMAN);

    Mica.MicaConfigDto dto = dtos.asDto(config);
    assertThat(dto).isNotNull();

    MicaConfig fromDto = dtos.fromDto(dto);
    assertThat(fromDto).isEqualToIgnoringGivenFields(config, "createdDate");
  }

  @AfterAll
  public static void tearDownSubject() {
    tearDownShiro();
  }
}
