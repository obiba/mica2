/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Study import never imports persons (#4752). The role restriction is tested in RequiresAnyRoleFeatureTest.
 */
@ExtendWith(MockitoExtension.class)
public class StudiesImportResourceTest {

  private static final String STUDY_ID = "s1";

  @InjectMocks
  private StudiesImportResource resource;

  @Mock
  private IndividualStudyService individualStudyService;

  @Mock
  private HarmonizationStudyService harmonizationStudyService;

  @Mock
  private StudyService studyService;

  @Mock
  private MicaConfigService micaConfigService;

  @Mock
  private Dtos dtos;

  /** serves the remote study */
  private HttpServer remote;

  private String remoteUrl;

  @BeforeEach
  public void setUp() throws IOException {
    remote = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    remote.createContext("/ws/draft/", exchange -> {
      byte[] body = ("{\"id\":\"" + STUDY_ID + "\"}").getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(body);
      }
    });
    remote.start();
    remoteUrl = "http://127.0.0.1:" + remote.getAddress().getPort();
    when(micaConfigService.getConfig()).thenReturn(new MicaConfig());
  }

  @AfterEach
  public void tearDown() {
    remote.stop(0);
  }

  @Test
  public void test_import_new_individual_study_clears_membership_sort_order() {
    Study remoteStudy = withSortOrder(new Study(), "remote-person");
    when(dtos.fromDto(any(Mica.StudyDtoOrBuilder.class))).thenReturn(remoteStudy);
    when(studyService.findStudy(STUDY_ID)).thenThrow(NoSuchEntityException.withId(Study.class, STUDY_ID));

    Response response = save("individual-study");

    assertSaved(response);
    ArgumentCaptor<Study> saved = ArgumentCaptor.forClass(Study.class);
    verify(individualStudyService).save(saved.capture());
    assertThat(saved.getValue().getMembershipSortOrder()).isEmpty();
  }

  @Test
  public void test_import_existing_individual_study_keeps_local_membership_sort_order() {
    Study remoteStudy = withSortOrder(new Study(), "remote-person");
    Study localStudy = withSortOrder(new Study(), "local-person");
    localStudy.setId(STUDY_ID);
    when(dtos.fromDto(any(Mica.StudyDtoOrBuilder.class))).thenReturn(remoteStudy);
    when(studyService.findStudy(STUDY_ID)).thenReturn(localStudy);
    when(individualStudyService.findStudy(STUDY_ID)).thenReturn(localStudy);

    Response response = save("individual-study");

    assertSaved(response);
    ArgumentCaptor<Study> saved = ArgumentCaptor.forClass(Study.class);
    verify(individualStudyService).save(saved.capture());
    assertThat(saved.getValue().getId()).isEqualTo(STUDY_ID);
    assertThat(saved.getValue().getMembershipSortOrder()).isEqualTo(Map.of("contact", List.of("local-person")));
  }

  @Test
  public void test_import_new_harmonization_study_clears_membership_sort_order() {
    HarmonizationStudy remoteStudy = withSortOrder(new HarmonizationStudy(), "remote-person");
    when(dtos.fromDto(any(Mica.StudyDtoOrBuilder.class))).thenReturn(remoteStudy);
    when(studyService.findStudy(STUDY_ID)).thenThrow(NoSuchEntityException.withId(HarmonizationStudy.class, STUDY_ID));

    Response response = save("harmonization-study");

    assertSaved(response);
    ArgumentCaptor<HarmonizationStudy> saved = ArgumentCaptor.forClass(HarmonizationStudy.class);
    verify(harmonizationStudyService).save(saved.capture());
    assertThat(saved.getValue().getMembershipSortOrder()).isEmpty();
  }

  @Test
  public void test_import_existing_harmonization_study_keeps_local_membership_sort_order() {
    HarmonizationStudy remoteStudy = withSortOrder(new HarmonizationStudy(), "remote-person");
    HarmonizationStudy localStudy = withSortOrder(new HarmonizationStudy(), "local-person");
    localStudy.setId(STUDY_ID);
    when(dtos.fromDto(any(Mica.StudyDtoOrBuilder.class))).thenReturn(remoteStudy);
    when(studyService.findStudy(STUDY_ID)).thenReturn(localStudy);
    when(harmonizationStudyService.findStudy(STUDY_ID)).thenReturn(localStudy);

    Response response = save("harmonization-study");

    assertSaved(response);
    ArgumentCaptor<HarmonizationStudy> saved = ArgumentCaptor.forClass(HarmonizationStudy.class);
    verify(harmonizationStudyService).save(saved.capture());
    assertThat(saved.getValue().getId()).isEqualTo(STUDY_ID);
    assertThat(saved.getValue().getMembershipSortOrder()).isEqualTo(Map.of("contact", List.of("local-person")));
  }

  private Response save(String type) {
    return resource.saveStudies(remoteUrl, "user", "password", type, List.of(STUDY_ID), Collections.emptyList());
  }

  @SuppressWarnings("unchecked")
  private static void assertSaved(Response response) {
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat((Map<String, Integer>) response.getEntity()).containsEntry(STUDY_ID, 200);
  }

  /** the contacts' display order, by person id */
  private static <T extends BaseStudy> T withSortOrder(T study, String personId) {
    study.setMembershipSortOrder(new HashMap<>(Map.of("contact", List.of(personId))));
    return study;
  }
}
