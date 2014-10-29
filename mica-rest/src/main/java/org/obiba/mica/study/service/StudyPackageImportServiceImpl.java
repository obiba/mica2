/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.jersey.protobuf.AbstractProtobufProvider;
import org.obiba.mica.core.domain.AttachmentAware;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.file.TempFileService;
import org.obiba.mica.network.NoSuchNetworkException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.googlecode.protobuf.format.JsonFormat;

@Service
public class StudyPackageImportServiceImpl extends AbstractProtobufProvider implements StudyPackageImportService {

  private static final Logger log = LoggerFactory.getLogger(StudyPackageImportServiceImpl.class);

  @Inject
  private TempFileService tempFileService;

  @Inject
  private StudyService studyService;

  @Inject
  private NetworkService networkService;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Inject
  private Dtos dtos;

  @Override
  public void importZip(InputStream inputStream, boolean publish) throws IOException {
    StudyPackage studyPackage = new StudyPackage(inputStream);
    if(studyPackage.study != null) {
      for(Map.Entry<String, ByteArrayInputStream> entry : studyPackage.attachments.entrySet()) {
        importStudyAttachment(studyPackage.study, entry.getKey(), entry.getValue());
      }
      importStudy(studyPackage.study, publish);
      studyPackage.networks.forEach(net -> importNetwork(net, publish));
      studyPackage.datasets.forEach(ds -> importDataset(ds, publish));
    }
  }

  private void importStudyAttachment(Study study, String attId, ByteArrayInputStream content) throws IOException {
    // look for the attachment name in the study
    Attachment attachment = getAttachment(study, attId);
    if(attachment == null) return;

    TempFile tempFile = new TempFile();
    tempFile.setId(attachment.getId());
    tempFile.setName(attachment.getName());
    tempFileService.addTempFile(tempFile, content);
  }

  private Attachment getAttachment(@NotNull Study study, @NotNull String attId) {
    if(study.getLogo() != null && study.getLogo().getId().equals(attId)) {
      return study.getLogo();
    }
    Attachment attachment = findAttachment(study, attId);
    if(attachment != null) return attachment;

    if(study.hasPopulations()) {
      for(Population population : study.getPopulations()) {
        attachment = getAttachment(population, attId);
        if(attachment != null) return attachment;
      }
    }
    return null;
  }

  private Attachment getAttachment(@NotNull Population population, @NotNull String attId) {
    if(!population.hasDataCollectionEvents()) return null;
    for(DataCollectionEvent dce : population.getDataCollectionEvents()) {
      Attachment attachment = findAttachment(dce, attId);
      if(attachment != null) return attachment;
    }
    return null;
  }

  private Attachment findAttachment(@NotNull AttachmentAware attachmentAware, @NotNull String attId) {
    if(!attachmentAware.hasAttachments()) return null;

    for(Attachment attachment : attachmentAware.getAttachments()) {
      if(attachment.getId().equals(attId)) return attachment;
    }
    return null;
  }

  private void importStudy(Study study, boolean publish) {
    if(study.getAcronym() == null) {
      study.setAcronym(study.getName().asAcronym());
    }
    studyService.save(study);
    if(publish) {
      studyService.publish(study.getId());
    }
  }

  private void importNetwork(Network network, boolean publish) {
    try {
      Network existing = networkService.findById(network.getId());
      network.getStudyIds().stream().filter(sid -> !existing.getStudyIds().contains(sid))
          .forEach(sid -> existing.getStudyIds().add(sid));
      networkService.save(existing);
    } catch(NoSuchNetworkException e) {
      networkService.save(network);
    }

    if(publish) networkService.publish(network.getId(), publish);
  }

  private void importDataset(Dataset dataset, boolean publish) {
    if(dataset instanceof StudyDataset) {
      importDataset((StudyDataset) dataset, publish);
    } else {
      importDataset((HarmonizationDataset) dataset, publish);
    }
  }

  private void importDataset(StudyDataset dataset, boolean publish) {
    if (Strings.isNullOrEmpty(dataset.getStudyTable().getStudyId())) return;
    try {
      studyDatasetService.findById(dataset.getId());
      studyDatasetService.save(dataset);
    } catch(NoSuchDatasetException e) {
      studyDatasetService.save(dataset);
    }
    if (publish) studyDatasetService.publish(dataset.getId(), publish);
  }

  private void importDataset(HarmonizationDataset dataset, boolean publish) {
    try {
      HarmonizationDataset existing = harmonizationDatasetService.findById(dataset.getId());
      // TODO merge study tables
      harmonizationDatasetService.save(existing);
    } catch(NoSuchDatasetException e) {
      harmonizationDatasetService.save(dataset);
    }
    if (publish) harmonizationDatasetService.publish(dataset.getId(), publish);
  }

  private final class StudyPackage {

    private Study study = null;

    private final List<Network> networks = Lists.newArrayList();

    private final List<Dataset> datasets = Lists.newArrayList();

    private final Map<String, ByteArrayInputStream> attachments = Maps.newHashMap();

    private StudyPackage(InputStream inputStream) {
      try(ZipInputStream zipIn = new ZipInputStream(inputStream)) {
        ZipEntry entry;
        while((entry = zipIn.getNextEntry()) != null) {
          readZipEntry(zipIn, entry);
        }
      } catch(Exception e) {
        log.error("Failed importing from zip", e);
        throw new RuntimeException("Failed importing from zip", e);
      }
    }

    private void readZipEntry(ZipInputStream zipIn, ZipEntry entry) throws IOException {
      if(entry.getName().endsWith("attachments/")) {
        zipIn.closeEntry();
        return;
      }

      if(entry.getName().contains("attachments/")) {
        String attId = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
        attachments.put(attId, new ByteArrayInputStream(readBytes(zipIn)));
      } else if(entry.getName().endsWith(".json")) {
        String name = entry.getName();
        int slash = name.lastIndexOf('/');
        if(slash > -1) {
          name = name.substring(slash + 1);
        }
        log.debug("Reading {}...", name);

        if(name.startsWith("study-")) {
          study = readStudy(zipIn);
        } else if(name.startsWith("dataset-")) {
          datasets.add(readDataset(zipIn));
        } else if(name.startsWith("network-")) {
          networks.add(readNetwork(zipIn));
        }
        zipIn.closeEntry();
      }
    }

    private Study readStudy(InputStream inputStream) throws IOException {
      Mica.StudyDto.Builder builder = Mica.StudyDto.newBuilder();
      Readable input = new InputStreamReader(inputStream, Charsets.UTF_8);
      JsonFormat.merge(input, builder);
      return dtos.fromDto(builder);
    }

    private Network readNetwork(InputStream inputStream) throws IOException {
      Mica.NetworkDto.Builder builder = Mica.NetworkDto.newBuilder();
      Readable input = new InputStreamReader(inputStream, Charsets.UTF_8);
      JsonFormat.merge(input, builder);
      return dtos.fromDto(builder);
    }

    private Dataset readDataset(InputStream inputStream) throws IOException {
      Mica.DatasetDto.Builder builder = Mica.DatasetDto.newBuilder();
      Readable input = new InputStreamReader(inputStream, Charsets.UTF_8);
      JsonFormat.merge(input, builder);
      return dtos.fromDto(builder);
    }

    private byte[] readBytes(ZipInputStream zipIn) throws IOException {
      ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
      ByteStreams.copy(zipIn, entryOut);
      entryOut.close();
      return entryOut.toByteArray();
    }

  }

}
