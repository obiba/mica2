package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.file.Attachment;
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

    attachmentRepository.findAll().stream().filter(Attachment::hasPath).forEach(a -> {
      String path = a.getPath().replaceFirst("/[0-9a-f\\-]+$", "");
      if(a.getPath().contains("/population/")) {
        path = path.replaceFirst("/attachment$","").replaceFirst("/population/","/attachments/population/");
      } else {
        path = path.replaceAll("/attachment/","/attachments/");
      }
      a.setPath(path);
      attachmentRepository.save(a);

    });
  }
}
