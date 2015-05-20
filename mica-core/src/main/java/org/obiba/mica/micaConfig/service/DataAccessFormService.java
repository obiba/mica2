package org.obiba.mica.micaConfig.service;

import java.util.Optional;

import javax.inject.Inject;

import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.repository.DataAccessFormRepository;
import org.springframework.stereotype.Component;

@Component
public class DataAccessFormService {

  @Inject
  GitService gitService;

  @Inject
  DataAccessFormRepository dataAccessFormRepository;

  public void createOrUpdateDataAccessForm(DataAccessForm dataAccessForm) {
    dataAccessForm.incrementRevisionsAhead();
    gitService.save(dataAccessForm);
    dataAccessFormRepository.save(dataAccessForm);
  }

  public Optional<DataAccessForm> findDataAccessForm() {
    return Optional.ofNullable(dataAccessFormRepository.findOne(DataAccessForm.DEFAULT_ID));
  }

  public void publish() {
    Optional<DataAccessForm> dataAccessForm = findDataAccessForm();
    dataAccessForm.ifPresent(d -> {
      d.setPublishedTag(gitService.tag(d));
      d.setRevisionsAhead(0);
      d.setRevisionStatus(RevisionStatus.DRAFT);
      dataAccessFormRepository.save(d);
    });
  }
}
