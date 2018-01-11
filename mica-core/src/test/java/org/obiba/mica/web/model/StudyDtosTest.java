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

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.obiba.mica.core.domain.Timestamped;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.IndividualStudyService;

import java.util.Arrays;
import java.util.Locale;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.obiba.mica.assertj.Assertions.assertThat;
import static org.obiba.mica.core.domain.LocalizedString.en;

@RunWith(MockitoJUnitRunner.class)
public class StudyDtosTest {

  @InjectMocks
  private StudyDtos studyDtos;

  @SuppressWarnings("unused")
  @Spy
  private LocalizedStringDtos localizedStringDtos;

  @Mock
  private MicaConfigService micaConfigService;

  @Mock
  private IndividualStudyService individualStudyService;

  @Mock
  private PermissionsDtos permissionsDtos;

  @Mock
  private AttachmentDtos attachmentDtos;

  @Before
  public void before() {
    MicaConfig config = new MicaConfig();
    config.setLocales(Arrays.asList(Locale.ENGLISH, Locale.FRENCH));
    when(micaConfigService.getConfig()).thenReturn(config);

    when(permissionsDtos.asDto(any(Study.class))).thenReturn(Mica.PermissionsDto.getDefaultInstance());
    when(individualStudyService.isPublished(anyString())).thenReturn(true);
  }

  @Test
  public void test_required_only_dto() throws Exception {
    Study study = new Study();
    study.setId(new ObjectId().toString());
    study.setName(en("Canadian Longitudinal Study on Aging"));
    study.setObjectives(en("The Canadian Longitudinal Study on Aging (CLSA) is a large, national, long-term study"));

    Mica.StudyDto dto = studyDtos.asDto(study, true);
    Study fromDto = (Study)studyDtos.fromDto(dto);
    assertTimestamps(study, dto);
    assertThat(fromDto).areFieldsEqualToEachOther(study);
  }

  @Test
  public void test_full_dto() throws Exception {

    when(attachmentDtos.asDto(any(Attachment.class))).thenReturn(Mica.AttachmentDto.newBuilder().setId("123").setFileName("logo123").build());
    when(attachmentDtos.fromDto(any(Mica.AttachmentDtoOrBuilder.class))).thenReturn(createAttachment());

    Study study = createStudy();

    Mica.StudyDto dto = studyDtos.asDto(study, true);
    Study fromDto = (Study)studyDtos.fromDto(dto);
    assertTimestamps(study, dto);
    assertThat(fromDto).isEqualTo(study);
  }

  private void assertTimestamps(Timestamped study, Mica.StudyDtoOrBuilder dto) {
    assertThat(dto.getTimestamps().getCreated()).isEqualTo(study.getCreatedDate().toString());
    assertThat(dto.getTimestamps().getLastUpdate())
      .isEqualTo(study.getLastModifiedDate() == null ? "" : study.getLastModifiedDate().toString());
  }

  private Study createStudy() {
    Study study = new Study();
    study.setId("study_1");
    study.setLogo(createAttachment());
    return study;
  }

  private Attachment createAttachment() {
    Attachment attachment = new Attachment();
    attachment.setId("123");
    attachment.setName("logo123");
    return attachment;
  }
}
