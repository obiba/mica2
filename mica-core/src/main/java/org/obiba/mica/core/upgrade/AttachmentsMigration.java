package org.obiba.mica.core.upgrade;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.bson.types.ObjectId;
import org.obiba.git.GitException;
import org.obiba.mica.core.domain.GitPersistable;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.repository.DataAccessFormRepository;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.domain.Study;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

@Component
public class AttachmentsMigration implements UpgradeStep {
  private static final Logger log = LoggerFactory.getLogger(AttachmentsMigration.class);

  @Inject
  private GitService gitService;

  @Inject
  private FileService fileService;

  @Inject
  private StudyRepository studyRepository;

  @Inject
  private AttachmentRepository attachmentRepository;

  @Inject
  private DataAccessFormRepository dataAccessFormRepository;

  @Override
  public String getDescription() {
    return "Migrate attachments from git to GridFS";
  }

  @Override
  public Version getAppliesTo() {
    return new Version("0.9");
  }

  @Override
  public void execute(Version version) {
    log.info("Executing attachments migration from git to GridFS");
    List<Study> studies = studyRepository.findAll();

    studies.forEach(study -> {
      Optional.ofNullable(study.getLogo()).ifPresent(l -> migrateAttachment(study, l));
      Set<String> attachmentIds = Sets.newHashSet();

      study.getPopulations()
        .forEach(p -> p.getDataCollectionEvents().forEach(dce -> dce.getAttachments().forEach(a -> {
          if(attachmentIds.contains(a.getId())) { //duplicate reference to same attachment
            log.info("Duplicate attachment found:" + a.getId());
            Attachment attachment = new Attachment();
            attachment.setType(a.getType());
            attachment.setName(a.getName());
            attachment.setLang(a.getLang());
            attachment.setMd5(a.getMd5());
            attachment.setSize(a.getSize());
            attachment.setType(a.getType());
            attachment.setDescription(a.getDescription());
            attachment.setJustUploaded(a.isJustUploaded());
            attachment.setId(new ObjectId().toString());
            attachmentRepository.save(attachment);
            migrateAttachment(study, a.getId(), attachment.getId());
            a.setId(attachment.getId());
          } else {
            migrateAttachment(study, a);
            attachmentIds.add(a.getId());
          }
        })));

      if(!attachmentIds.isEmpty()) {
        studyRepository.save(study);
        gitService.save(study);
      }
    });

    List<DataAccessForm> forms = dataAccessFormRepository.findAll();

    forms.forEach(daf -> daf.getPdfTemplates().forEach((k, a) -> migrateAttachment(daf, a)));
  }

  private void migrateAttachment(GitPersistable persistable, Attachment attachment) {
    migrateAttachment(persistable, attachment.getId(), attachment.getId());
  }

  private void migrateAttachment(GitPersistable persistable, String sourceId, String destId) {
    try {
      byte[] ba = gitService.readFileHead(persistable, sourceId);
      fileService.save(destId, new ByteArrayInputStream(ba));
    } catch(GitException ex) {
      if(!(ex.getCause() instanceof FileNotFoundException)) {
        throw ex;
      }
    }
  }
}

