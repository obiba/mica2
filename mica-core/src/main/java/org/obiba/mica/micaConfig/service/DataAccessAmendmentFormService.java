package org.obiba.mica.micaConfig.service;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.inject.Inject;

import org.obiba.mica.micaConfig.domain.DataAccessAmendmentForm;
import org.obiba.mica.micaConfig.repository.DataAccessAmendmentFormRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class DataAccessAmendmentFormService extends AbstractDataAccessEntityFormService<DataAccessAmendmentForm> {

  private DataAccessAmendmentFormRepository dataAccessAmendmentFormRepository;

  @Inject
  public DataAccessAmendmentFormService(DataAccessAmendmentFormRepository dataAccessAmendmentFormRepository) {
    this.dataAccessAmendmentFormRepository = dataAccessAmendmentFormRepository;
  }

  @Override
  public DataAccessAmendmentForm createOrUpdate(DataAccessAmendmentForm dataAccessForm) {
    validateForm(dataAccessForm);
    dataAccessForm.setRevision(0);
    dataAccessForm.setLastUpdateDate(LocalDateTime.now());
    return dataAccessAmendmentFormRepository.save(dataAccessForm);
  }

  @Override
  public DataAccessAmendmentForm findDraft() {
    Optional<DataAccessAmendmentForm> form = dataAccessAmendmentFormRepository.findById(DataAccessAmendmentForm.DEFAULT_ID);
    if (!form.isPresent()) {
      createOrUpdate(createDefaultDataAccessAmendmentForm());
      form = dataAccessAmendmentFormRepository.findById(DataAccessAmendmentForm.DEFAULT_ID);
    }
    return form.get();
  }

  @Override
  DataAccessAmendmentForm findLatest() {
    Optional<DataAccessAmendmentForm> latest = findFirstSortByRevisionDesc();
    if (!latest.isPresent()) {
      publish();
      latest = findFirstSortByRevisionDesc();
    }
    return latest.get();
  }

  @Override
  DataAccessAmendmentForm findByRevision(int revision) {
    return dataAccessAmendmentFormRepository.findFirstByRevision(revision);
  }

  @Override
  public void publish() {
    DataAccessAmendmentForm draft = findDraft();
    draft.setId(null);
    Optional<DataAccessAmendmentForm> latest = findFirstSortByRevisionDesc();
    draft.setRevision(latest.isPresent() ? latest.get().getRevision() + 1 : 1);
    dataAccessAmendmentFormRepository.save(draft);
  }

  @Override
  String getDataAccessEntityFormResourceLocation() {
    return "classpath:config/data-access-amendment-form/";
  }

  //
  // Private methods
  //

  private Optional<DataAccessAmendmentForm> findFirstSortByRevisionDesc() {
    return dataAccessAmendmentFormRepository.findAll(Sort.by(Sort.Order.desc("revision"))).stream()
      .filter(form -> form.getRevision()>0)
      .findFirst();
  }

  private DataAccessAmendmentForm createDefaultDataAccessAmendmentForm() {
    DataAccessAmendmentForm form = new DataAccessAmendmentForm();
    form.setDefinition(getDefaultDataAccessFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDataAccessFormResourceAsString("schema.json"));
    form.setTitleFieldPath("projectTitle");
    form.setSummaryFieldPath("summary");
    form.setEndDateFieldPath("endDate");
    form.setRevision(0);
    return form;
  }
}
