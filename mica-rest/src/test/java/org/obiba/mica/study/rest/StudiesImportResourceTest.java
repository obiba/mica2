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
import java.util.Set;

import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Study import is restricted to the global roles and never imports persons (#4752).
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

  @Mock
  private Subject subject;

  /** serves the remote study */
  private HttpServer remote;

  private String remoteUrl;

  @BeforeEach
  public void setUp() throws IOException {
    ThreadContext.bind(subject);
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
  }

  @AfterEach
  public void tearDown() {
    remote.stop(0);
    ThreadContext.unbindSubject();
  }

  /**
   * A mica-external-editor has ADD on every study type but none of the global roles.
   */
  @Test
  public void test_import_endpoints_forbidden_without_global_role() {
    withRoles(Roles.MICA_EXTERNAL_EDITOR, Roles.MICA_USER);

    List<Executable> endpoints = List.of(
      () -> resource.listDifferences(remoteUrl, "user", "password", "individual-study"),
      () -> resource.listRemoteStudies(remoteUrl, "user", "password", "individual-study"),
      () -> resource.checkIfAlreadyExistsLocally(List.of(STUDY_ID), "individual-study"),
      () -> save("individual-study"));

    for (Executable endpoint : endpoints) {
      assertThatThrownBy(endpoint::execute).isInstanceOf(ForbiddenException.class);
    }
    verifyNoInteractions(micaConfigService, studyService, individualStudyService, harmonizationStudyService, dtos);
  }

  /**
   * Any one of the global roles is enough (the obiba-jersey-shiro filter would require all of them).
   */
  @Test
  public void test_import_allowed_with_any_global_role() {
    mockConfig();
    for (String role : List.of(Roles.MICA_ADMIN, Roles.MICA_REVIEWER, Roles.MICA_EDITOR)) {
      withRoles(role);
      Response response = resource.checkIfAlreadyExistsLocally(Collections.emptyList(), "individual-study");
      assertThat(response.getStatus()).as(role).isEqualTo(200);
    }
  }

  @Test
  public void test_import_new_individual_study_clears_membership_sort_order() {
    withRoles(Roles.MICA_EDITOR);
    mockConfig();
    Study remoteStudy = withRemoteSortOrder(new Study());
    when(dtos.fromDto(any(Mica.StudyDtoOrBuilder.class))).thenReturn(remoteStudy);
    when(studyService.findStudy(STUDY_ID)).thenThrow(NoSuchEntityException.withId(Study.class, STUDY_ID));

    Response response = save("individual-study");

    assertSaved(response);
    ArgumentCaptor<Study> saved = ArgumentCaptor.forClass(Study.class);
    verify(individualStudyService).save(saved.capture());
    assertThat(saved.getValue().getMembershipSortOrder()).isEmpty();
  }

  @Test
  public void test_import_existing_individual_study_clears_membership_sort_order() {
    withRoles(Roles.MICA_EDITOR);
    mockConfig();
    Study remoteStudy = withRemoteSortOrder(new Study());
    Study localStudy = new Study();
    localStudy.setId(STUDY_ID);
    when(dtos.fromDto(any(Mica.StudyDtoOrBuilder.class))).thenReturn(remoteStudy);
    when(studyService.findStudy(STUDY_ID)).thenReturn(localStudy);
    when(individualStudyService.findStudy(STUDY_ID)).thenReturn(localStudy);

    Response response = save("individual-study");

    assertSaved(response);
    ArgumentCaptor<Study> saved = ArgumentCaptor.forClass(Study.class);
    verify(individualStudyService).save(saved.capture());
    assertThat(saved.getValue().getId()).isEqualTo(STUDY_ID);
    assertThat(saved.getValue().getMembershipSortOrder()).isEmpty();
  }

  @Test
  public void test_import_harmonization_study_clears_membership_sort_order() {
    withRoles(Roles.MICA_EDITOR);
    mockConfig();
    HarmonizationStudy remoteStudy = withRemoteSortOrder(new HarmonizationStudy());
    when(dtos.fromDto(any(Mica.StudyDtoOrBuilder.class))).thenReturn(remoteStudy);
    when(studyService.findStudy(STUDY_ID)).thenThrow(NoSuchEntityException.withId(HarmonizationStudy.class, STUDY_ID));

    Response response = save("harmonization-study");

    assertSaved(response);
    ArgumentCaptor<HarmonizationStudy> saved = ArgumentCaptor.forClass(HarmonizationStudy.class);
    verify(harmonizationStudyService).save(saved.capture());
    assertThat(saved.getValue().getMembershipSortOrder()).isEmpty();
  }

  /** the subject has exactly these roles */
  private void withRoles(String... roles) {
    Set<String> granted = Set.of(roles);
    lenient().when(subject.hasRole(anyString())).thenAnswer(invocation -> granted.contains(invocation.<String>getArgument(0)));
  }

  private void mockConfig() {
    when(micaConfigService.getConfig()).thenReturn(new MicaConfig());
  }

  private Response save(String type) {
    return resource.saveStudies(remoteUrl, "user", "password", type, List.of(STUDY_ID), Collections.emptyList());
  }

  @SuppressWarnings("unchecked")
  private static void assertSaved(Response response) {
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat((Map<String, Integer>) response.getEntity()).containsEntry(STUDY_ID, 200);
  }

  /** the remote sort order holds remote person ids */
  private static <T extends BaseStudy> T withRemoteSortOrder(T study) {
    study.setMembershipSortOrder(new HashMap<>(Map.of("contact", List.of("remote-person"))));
    return study;
  }
}
