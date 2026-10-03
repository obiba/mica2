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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.network.NoSuchNetworkException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.obiba.mica.core.domain.LocalizedString.en;

/**
 * The publication done by the study package import (from its REST endpoint or the seed folder) is not notified by
 * email.
 */
public class StudyPackageImportServiceImplTest {

  private StudyPackageImportServiceImpl importService;

  private StudyService studyService;

  private NetworkService networkService;

  private CollectedDatasetService collectedDatasetService;

  private HarmonizedDatasetService harmonizedDatasetService;

  @BeforeEach
  public void setUp() {
    studyService = mock(StudyService.class);
    networkService = mock(NetworkService.class);
    collectedDatasetService = mock(CollectedDatasetService.class);
    harmonizedDatasetService = mock(HarmonizedDatasetService.class);
    when(networkService.findById(anyString())).thenThrow(NoSuchNetworkException.withId("n1"));

    Dtos dtos = mock(Dtos.class);
    when(dtos.fromDto(any(Mica.StudyDtoOrBuilder.class))).thenAnswer(i -> study());
    when(dtos.fromDto(any(Mica.NetworkDtoOrBuilder.class))).thenAnswer(i -> network());
    when(dtos.fromDto(any(Mica.DatasetDto.class))).thenReturn(collectedDataset(), harmonizedDataset());

    importService = new StudyPackageImportServiceImpl();
    ReflectionTestUtils.setField(importService, "fileSystemService", mock(FileSystemService.class));
    ReflectionTestUtils.setField(importService, "tempFileService", mock(TempFileService.class));
    ReflectionTestUtils.setField(importService, "studyService", studyService);
    ReflectionTestUtils.setField(importService, "networkService", networkService);
    ReflectionTestUtils.setField(importService, "collectedDatasetService", collectedDatasetService);
    ReflectionTestUtils.setField(importService, "harmonizedDatasetService", harmonizedDatasetService);
    ReflectionTestUtils.setField(importService, "dtos", dtos);
  }

  @Test
  public void test_import_publishes_without_notification() throws IOException {
    importService.importZip(studyPackage(), true);

    verify(studyService).publish("s1", true, PublishCascadingScope.ALL, false);
    verify(networkService).publish("n1", true, PublishCascadingScope.ALL, false);
    verify(collectedDatasetService).publish("cds1", true, PublishCascadingScope.ALL, false);
    verify(harmonizedDatasetService).publish("hds1", true, PublishCascadingScope.ALL, false);

    // the overloads that notify
    verify(studyService, never()).publish(anyString(), anyBoolean(), any(PublishCascadingScope.class));
    verify(networkService, never()).publish(anyString(), anyBoolean(), any(PublishCascadingScope.class));
    verify(collectedDatasetService, never()).publish(anyString(), anyBoolean(), any(PublishCascadingScope.class));
    verify(harmonizedDatasetService, never()).publish(anyString(), anyBoolean(), any(PublishCascadingScope.class));
  }

  @Test
  public void test_import_without_publication() throws IOException {
    importService.importZip(studyPackage(), false);

    verify(studyService, never()).publish(anyString(), anyBoolean(), any(PublishCascadingScope.class), anyBoolean());
    verify(networkService, never()).publish(anyString(), anyBoolean(), any(PublishCascadingScope.class), anyBoolean());
    verify(collectedDatasetService, never())
      .publish(anyString(), anyBoolean(), any(PublishCascadingScope.class), anyBoolean());
    verify(harmonizedDatasetService, never())
      .publish(anyString(), anyBoolean(), any(PublishCascadingScope.class), anyBoolean());
  }

  //
  // Private methods
  //

  /**
   * A package with a study, a network and two datasets, the DTOs being converted by the mocked {@link Dtos}.
   */
  private ByteArrayInputStream studyPackage() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try(ZipOutputStream zip = new ZipOutputStream(out)) {
      addEntry(zip, "study-s1.json", "{}");
      addEntry(zip, "network-n1.json", "{}");
      addEntry(zip, "dataset-cds1.json", "{\"entityType\": \"Collected\"}");
      addEntry(zip, "dataset-hds1.json", "{\"entityType\": \"Harmonized\"}");
    }
    return new ByteArrayInputStream(out.toByteArray());
  }

  private void addEntry(ZipOutputStream zip, String name, String content) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(content.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }

  private Study study() {
    Study study = new Study();
    study.setName(en("Study One"));
    study.setAcronym(en("S1"));
    return study;
  }

  private Network network() {
    Network network = new Network();
    network.setName(en("Network One"));
    network.setAcronym(en("N1"));
    return network;
  }

  private StudyDataset collectedDataset() {
    StudyTable table = new StudyTable();
    table.setStudyId("s1");
    StudyDataset dataset = new StudyDataset();
    dataset.setId("cds1");
    dataset.setStudyTable(table);
    return dataset;
  }

  private HarmonizationDataset harmonizedDataset() {
    HarmonizationDataset dataset = new HarmonizationDataset();
    dataset.setId("hds1");
    return dataset;
  }
}
