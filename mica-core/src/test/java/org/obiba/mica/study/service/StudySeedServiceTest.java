/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.study.domain.Study;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The publication of the studies found in the seed folder is not notified by email.
 */
public class StudySeedServiceTest {

  @TempDir
  Path seedRepository;

  private StudySeedService seedService;

  private IndividualStudyService individualStudyService;

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() throws IOException {
    Files.createDirectories(seedRepository.resolve("in"));

    individualStudyService = mock(IndividualStudyService.class);
    objectMapper = mock(ObjectMapper.class);

    seedService = new StudySeedService();
    ReflectionTestUtils.setField(seedService, "individualStudyService", individualStudyService);
    ReflectionTestUtils.setField(seedService, "studyPackageImportService", mock(StudyPackageImportService.class));
    ReflectionTestUtils.setField(seedService, "objectMapper", objectMapper);
    ReflectionTestUtils.setField(seedService, "seedRepository", seedRepository.toFile());
  }

  @Test
  public void test_study_seed_publishes_without_notification() throws IOException {
    when(objectMapper.readValue(any(InputStream.class), eq(Study.class))).thenReturn(study("s1"));
    addSeed("study-s1.json");

    seedService.importSeed();

    verify(individualStudyService).publish("s1", true, PublishCascadingScope.NONE, false);
    verifyNotifyingOverloadsNotCalled();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void test_studies_seed_publishes_without_notification() throws IOException {
    when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
      .thenReturn(List.of(study("s1"), study("s2")));
    addSeed("studies.json");

    seedService.importSeed();

    verify(individualStudyService).publish("s1", true, PublishCascadingScope.NONE, false);
    verify(individualStudyService).publish("s2", true, PublishCascadingScope.NONE, false);
    verifyNotifyingOverloadsNotCalled();
  }

  //
  // Private methods
  //

  private void verifyNotifyingOverloadsNotCalled() {
    verify(individualStudyService, never()).publish(anyString(), anyBoolean());
    verify(individualStudyService, never()).publish(anyString(), anyBoolean(), any(PublishCascadingScope.class));
  }

  private void addSeed(String name) throws IOException {
    File seed = seedRepository.resolve("in").resolve(name).toFile();
    Files.writeString(seed.toPath(), "{}");
  }

  private Study study(String id) {
    Study study = new Study();
    study.setId(id);
    return study;
  }
}
