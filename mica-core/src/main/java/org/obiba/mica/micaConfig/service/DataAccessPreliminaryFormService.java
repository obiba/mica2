package org.obiba.mica.micaConfig.service;

import org.obiba.mica.micaConfig.domain.DataAccessPreliminaryForm;
import org.obiba.mica.micaConfig.repository.DataAccessPreliminaryFormRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class DataAccessPreliminaryFormService extends AbstractDataAccessEntityFormService<DataAccessPreliminaryForm> {

  private DataAccessPreliminaryFormRepository dataAccessPreliminaryFormRepository;

  @Inject
  public DataAccessPreliminaryFormService(DataAccessPreliminaryFormRepository dataAccessPreliminaryFormRepository) {
    this.dataAccessPreliminaryFormRepository = dataAccessPreliminaryFormRepository;
  }

  @Override
  public DataAccessPreliminaryForm createOrUpdate(DataAccessPreliminaryForm dataAccessForm) {
    validateForm(dataAccessForm);
    dataAccessForm.setRevision(0);
    dataAccessForm.setLastUpdateDate(LocalDateTime.now());
    return dataAccessPreliminaryFormRepository.save(dataAccessForm);
  }

  @Override
  public DataAccessPreliminaryForm findDraft() {
    Optional<DataAccessPreliminaryForm> form = dataAccessPreliminaryFormRepository.findById(DataAccessPreliminaryForm.DEFAULT_ID);
    if (!form.isPresent()) {
      createOrUpdate(createDefaultDataAccessPreliminaryForm());
      form = dataAccessPreliminaryFormRepository.findById(DataAccessPreliminaryForm.DEFAULT_ID);
    }
    return form.get();
  }

  @Override
  DataAccessPreliminaryForm findLatest() {
    Optional<DataAccessPreliminaryForm> latest = findFirstSortByRevisionDesc();
    if (!latest.isPresent()) {
      publish();
      latest = findFirstSortByRevisionDesc();
    }
    return latest.get();
  }

  @Override
  DataAccessPreliminaryForm findByRevision(int revision) {
    return dataAccessPreliminaryFormRepository.findFirstByRevision(revision);
  }

  @Override
  public void publish() {
    DataAccessPreliminaryForm draft = findDraft();
    draft.setId(null);
    Optional<DataAccessPreliminaryForm> latest = findFirstSortByRevisionDesc();
    draft.setRevision(latest.isPresent() ? latest.get().getRevision() + 1 : 1);
    dataAccessPreliminaryFormRepository.save(draft);
  }

  @Override
  String getDataAccessEntityFormResourceLocation() {
    return "classpath:config/data-access-preliminary-form/";
  }

  //
  // Private methods
  //

  private Optional<DataAccessPreliminaryForm> findFirstSortByRevisionDesc() {
    return dataAccessPreliminaryFormRepository.findAll(Sort.by(Sort.Order.desc("revision"))).stream()
      .filter(form -> form.getRevision()>0)
      .findFirst();
  }

  private DataAccessPreliminaryForm createDefaultDataAccessPreliminaryForm() {
    DataAccessPreliminaryForm form = new DataAccessPreliminaryForm();
    form.setDefinition(getDefaultDataAccessFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDataAccessFormResourceAsString("schema.json"));
    form.setTitleFieldPath("projectTitle");
    form.setSummaryFieldPath("summary");
    form.setEndDateFieldPath("endDate");
    form.setRevision(0);
    return form;
  }
}
