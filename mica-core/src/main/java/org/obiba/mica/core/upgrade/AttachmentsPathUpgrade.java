package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import org.obiba.mica.core.repository.AttachmentRepository;
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
    return new Version("0.9.2");
  }

  @Override
  public void execute(Version version) {
    log.info("Executing attachments path property upgrade");

    attachmentRepository.findAll().stream().filter(a -> a.getPath().contains("/attachment/")).forEach(a -> {
      a.setPath(a.getPath().replaceAll("/attachment/[0-9a-f\\-]+$", ""));
      fileSystemService.save(a);
    });
  }
}
