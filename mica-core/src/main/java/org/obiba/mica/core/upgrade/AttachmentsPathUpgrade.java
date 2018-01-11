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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.file.InvalidFileNameException;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AttachmentsPathUpgrade implements UpgradeStep {
  private static final Logger log = LoggerFactory.getLogger(AttachmentsPathUpgrade.class);

  @Inject
  private AttachmentRepository attachmentRepository;

  @Inject
  private FileSystemService fileSystemService;

  @Override
  public String getDescription() {
    return "Changed attachments path property";
  }

  @Override
  public Version getAppliesTo() {
    return new Version("1.0.0");
  }

  @Override
  public void execute(Version version) {
    log.info("Executing attachments path property upgrade");

    Pattern pattern = Pattern.compile("[\\$%/#]");

    attachmentRepository.findAll().forEach(a -> {
      boolean isModified = false;

      if(a.getPath() != null && a.getPath().contains("/attachment/")) {
        a.setPath(a.getPath().replaceAll("/attachment/[0-9a-f\\-]+$", ""));
        isModified = true;
      }

      Matcher matcher = pattern.matcher(a.getName());

      if(matcher.find()) {
        a.setName(matcher.replaceAll("_"));
        isModified = true;
      }

      if(isModified) fileSystemService.save(a);
    });
  }
}
