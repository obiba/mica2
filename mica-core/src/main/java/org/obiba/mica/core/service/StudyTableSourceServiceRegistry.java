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
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.micaConfig.service.PluginsService;
import org.obiba.mica.spi.source.StudyTableFileSource;
import org.obiba.mica.spi.source.StudyTableSource;
import org.obiba.mica.spi.source.StudyTableSourceService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class StudyTableSourceServiceRegistry {

  private static final Logger log = LoggerFactory.getLogger(StudyTableSourceServiceRegistry.class);

  @Inject
  private PluginsService pluginsService;

  @Inject
  private OpalService opalService;

  @Inject
  protected FileSystemService fileSystemService;

  @Inject
  private FileStoreService fileStoreService;

  private Cache<String, StudyTableSource> sourcesCache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(1, TimeUnit.MINUTES).build();

  public synchronized StudyTableSource makeStudyTableSource(BaseStudy study, String source) {
    String cacheKey = String.format("%s::%s", study.getId(), source);
    try {
      return sourcesCache.get(cacheKey, () -> makeStudyTableSourceInternal(study, source));
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    }
  }
  private StudyTableSource makeStudyTableSourceInternal(BaseStudy study, String source) {
    if (OpalTableSource.isFor(source)) {
      OpalTableSource tableSource = OpalTableSource.fromURN(source);
      tableSource.initialise(opalService, study.getOpal());
      return tableSource;
    }
    if (ExcelTableSource.isFor(source)) {
      ExcelTableSource tableSource = ExcelTableSource.fromURN(source);
      tableSource.initialise(getFileInputStream(study, tableSource.getPath()));
      return tableSource;
    }
    Optional<StudyTableSourceService> serviceOptional = pluginsService.getStudyTableSourceServices().stream()
      .filter(service -> service.isFor(source)).findFirst();
    if (serviceOptional.isPresent()) {
      // TODO add a context to the study table source
      StudyTableSource tableSource = serviceOptional.get().makeSource(source);
      if (tableSource instanceof StudyTableFileSource) {
        StudyTableFileSource fileSource = (StudyTableFileSource)tableSource;
        fileSource.initialise(getFileInputStream(study, fileSource.getPath()));
      }
      return tableSource;
    }
    throw new NoSuchElementException("Missing study-table-source plugin to handle source: " + source);
  }

  private InputStream getFileInputStream(BaseStudy study, String path) {
    String fullPath = path;
    if (!fullPath.startsWith("/")) {
      // not a full path, then it is relative to the study's folder
      fullPath = String.format("/%s-study/%s/%s", (study instanceof Study ? "individual" : "harmonization"), study.getId(), path);
    }
    log.info("Reading study table from file: {}", fullPath);
    Pair<String, String> pathName = FileSystemService.extractPathName(fullPath);
    try {
      AttachmentState state = fileSystemService.getAttachmentState(pathName.getKey(), pathName.getValue(), false);
      return fileStoreService.getFile(state.getAttachment().getFileReference());
    } catch (Exception e) {
      log.error("Cannot read study table file: {}", fullPath , e);
      throw new NoSuchValueTableException("No value table at " + fullPath);
    }
  }

}
