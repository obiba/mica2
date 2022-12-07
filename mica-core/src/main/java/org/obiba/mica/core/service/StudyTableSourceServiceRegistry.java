/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.math3.util.Pair;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.mica.core.source.ExcelTableSource;
import org.obiba.mica.core.source.OpalTableSource;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.micaConfig.service.PluginsService;
import org.obiba.mica.spi.source.*;
import org.obiba.mica.study.domain.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class StudyTableSourceServiceRegistry {

  private static final Logger log = LoggerFactory.getLogger(StudyTableSourceServiceRegistry.class);

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private PluginsService pluginsService;

  @Inject
  private OpalService opalService;

  @Inject
  protected FileSystemService fileSystemService;

  @Inject
  private FileStoreService fileStoreService;

  private Cache<String, StudyTableSource> sourcesCache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(1, TimeUnit.MINUTES).build();

  public synchronized StudyTableSource makeStudyTableSource(IDataset dataset, IStudy study, String source) {
    StudyTableContext context = new StudyTableContext(dataset, study, micaConfigService.getConfig().getPrivacyThreshold());

    String cacheKey = String.format("%s::%s", study.getId(), source);
    try {
      return sourcesCache.get(cacheKey, () -> makeStudyTableSourceInternal(context, source));
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    }
  }
  private StudyTableSource makeStudyTableSourceInternal(StudyTableContext context, String source) {
    if (OpalTableSource.isFor(source)) {
      OpalTableSource tableSource = OpalTableSource.fromURN(source);
      tableSource.setStudyTableContext(context);
      tableSource.initialise(opalService);
      return tableSource;
    }
    if (ExcelTableSource.isFor(source)) {
      ExcelTableSource tableSource = ExcelTableSource.fromURN(source);
      tableSource.setStudyTableContext(context);
      tableSource.initialise(getFileInputStream(context, tableSource.getPath()));
      return tableSource;
    }
    Optional<StudyTableSourceService> serviceOptional = pluginsService.getStudyTableSourceServices().stream()
      .filter(service -> service.isFor(source)).findFirst();
    if (serviceOptional.isPresent()) {
      StudyTableSource tableSource = serviceOptional.get().makeSource(source);
      tableSource.setStudyTableContext(context);
      if (tableSource instanceof StudyTableFileSource) {
        StudyTableFileSource fileSource = (StudyTableFileSource)tableSource;
        fileSource.initialise(getFileInputStream(context, fileSource.getPath()));
      }
      return tableSource;
    }
    throw new NoSuchElementException("Missing study-table-source plugin to handle source: " + source);
  }

  private InputStream getFileInputStream(StudyTableContext context, String path) {
    String fullPath = path;
    Optional<AttachmentState> attachmentState;
    if (!fullPath.startsWith("/")) {
      // not a full path, then it may be relative to the dataset's folder
      fullPath = String.format("/%s-dataset/%s/%s", (context.getDataset() instanceof StudyDataset ? "collected" : "harmonized"), context.getDataset().getId(), path);
      attachmentState = getAttachmentState(fullPath);
      // not found, then try a path relative to the study's folder
      if (!attachmentState.isPresent()) {
        fullPath = String.format("/%s-study/%s/%s", (context.getStudy() instanceof Study ? "individual" : "harmonization"), context.getStudy().getId(), path);
        attachmentState = getAttachmentState(fullPath);
      }
    } else {
      attachmentState = getAttachmentState(fullPath);
    }
    if (attachmentState.isPresent()) {
      return fileStoreService.getFile(attachmentState.get().getAttachment().getFileReference());
    } else {
      throw new NoSuchValueTableException("No value table at " + fullPath);
    }
  }

  private Optional<AttachmentState> getAttachmentState(String fullPath) {
    log.info("Reading study table from file: {}", fullPath);
    Pair<String, String> pathName = FileSystemService.extractPathName(fullPath);
    try {
      AttachmentState state = fileSystemService.getAttachmentState(pathName.getKey(), pathName.getValue(), false);
      return Optional.of(state);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

}
