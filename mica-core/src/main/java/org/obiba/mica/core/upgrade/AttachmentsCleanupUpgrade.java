/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.upgrade;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.obiba.mica.access.DataAccessRequestRepository;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.study.StudyRepository;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

@Component
public class AttachmentsCleanupUpgrade implements UpgradeStep {
  private static final Logger log = LoggerFactory.getLogger(AttachmentsCleanupUpgrade.class);

  @Inject
  private AttachmentRepository attachmentRepository;

  @Inject
  private StudyRepository studyRepository;

  @Inject
  private DataAccessRequestRepository dataAccessRequestRepository;

  @Inject
  private FileStoreService fileStoreService;

  @Override
  public String getDescription() {
    return "Orphaned attachments cleanup";
  }

  @Override
  public Version getAppliesTo() {
    return new Version("1.0.0");
  }

  @Override
  public void execute(Version version) {
    log.info("Executing orphaned attachments cleanup");
    deleteOrphaned(studyRepository, "^/individual-study/([^/]+)/");
    deleteOrphaned(dataAccessRequestRepository, "^/data-access-request/([^/]+)/");
  }

  private <T extends MongoRepository> void deleteOrphaned(T repository, String idPattern) {
    Pattern pattern = Pattern.compile(idPattern);
    Set<String> missingIds = Sets.newHashSet();

    attachmentRepository.findAll().forEach(a -> {
      if(a.getPath() != null) {
        Matcher m = pattern.matcher(a.getPath());

        if(m.find()) {
          String id = m.group(1);

          if(missingIds.contains(id) || repository.findOne(id) == null) {
            if(!missingIds.contains(id)) missingIds.add(id);
            attachmentRepository.delete(a);
            fileStoreService.delete(a.getId());
          }
        }
      } else {
        attachmentRepository.delete(a);
        fileStoreService.delete(a.getId());
      }
    });
  }
}
