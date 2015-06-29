package org.obiba.mica.core.upgrade;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.obiba.git.GitException;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.file.GridFsService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.repository.DataAccessFormRepository;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.domain.Study;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AttachmentsMigration implements UpgradeStep {
  private static final Logger log = LoggerFactory.getLogger(AttachmentsMigration.class);

  @Inject
  private GitService gitService;

  @Inject
  private GridFsService gridFsService;

  @Inject
  private StudyRepository studyRepository;

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
      Optional.ofNullable(study.getLogo()).ifPresent(l -> {
        try {
          byte[] ba = gitService.readFileHead(study, l.getId());
          gridFsService.save(new ByteArrayInputStream(ba), l.getId());
        } catch(GitException ex) {
          if(!(ex.getCause() instanceof FileNotFoundException)) {
            throw ex;
          }
        }
      });

      study.getAllAttachments().forEach(a -> {
        try {
          byte[] ba = gitService.readFileHead(study, a.getId());
          gridFsService.save(new ByteArrayInputStream(ba), a.getId());
        } catch(GitException ex) {
          if(!(ex.getCause() instanceof FileNotFoundException)) {
            throw ex;
          }
        }
      }); //TODO: manually remove attachments from git
    });

    List<DataAccessForm> forms = dataAccessFormRepository.findAll();

    forms.forEach(daf -> {
      daf.getPdfTemplates().forEach( (k, a) -> {
        try {
          byte[] ba = gitService.readFileHead(daf, a.getId());
          gridFsService.save(new ByteArrayInputStream(ba), a.getId());
        } catch(GitException ex) {
          if(!(ex.getCause() instanceof FileNotFoundException)) {
            throw ex;
          }
        }
      });
    });
  }
}

