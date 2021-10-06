package org.obiba.mica.micaConfig.service;

import org.joda.time.DateTime;
import org.obiba.mica.micaConfig.domain.DataAccessFeasibilityForm;
import org.obiba.mica.micaConfig.repository.DataAccessFeasibilityFormRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

@Component
public class DataAccessFeasibilityFormService extends AbstractDataAccessEntityFormService<DataAccessFeasibilityForm> {

  private DataAccessFeasibilityFormRepository dataAccessFeasibilityFormRepository;

  @Inject
  public DataAccessFeasibilityFormService(DataAccessFeasibilityFormRepository dataAccessFeasibilityFormRepository) {
    this.dataAccessFeasibilityFormRepository = dataAccessFeasibilityFormRepository;
  }

  @Override
  public DataAccessFeasibilityForm createOrUpdate(DataAccessFeasibilityForm dataAccessForm) {
    validateForm(dataAccessForm);
    dataAccessForm.setRevision(0);
    dataAccessForm.setLastUpdateDate(DateTime.now());
    return dataAccessFeasibilityFormRepository.save(dataAccessForm);
  }

  @Override
  public DataAccessFeasibilityForm findDraft() {
    DataAccessFeasibilityForm form = dataAccessFeasibilityFormRepository.findOne(DataAccessFeasibilityForm.DEFAULT_ID);
    if (form == null) {
      createOrUpdate(createDefaultDataAccessFeasibilityForm());
      form = dataAccessFeasibilityFormRepository.findOne(DataAccessFeasibilityForm.DEFAULT_ID);
    }
    return form;
  }

  @Override
  DataAccessFeasibilityForm findLatest() {
    Optional<DataAccessFeasibilityForm> latest = findFirstSortByRevisionDesc();
    if (!latest.isPresent()) {
      publish();
      latest = findFirstSortByRevisionDesc();
    }
    return latest.get();
  }

  @Override
  DataAccessFeasibilityForm findByRevision(int revision) {
    return dataAccessFeasibilityFormRepository.findFirstByRevision(revision);
  }

  @Override
  public void publish() {
    DataAccessFeasibilityForm draft = findDraft();
    draft.setId(null);
    Optional<DataAccessFeasibilityForm> latest = findFirstSortByRevisionDesc();
    draft.setRevision(latest.isPresent() ? latest.get().getRevision() + 1 : 1);
    dataAccessFeasibilityFormRepository.save(draft);
  }

  @Override
  String getDataAccessEntityFormResourceLocation() {
    return "classpath:config/data-access-feasibility-form/";
  }

  //
  // Private methods
  //

  private Optional<DataAccessFeasibilityForm> findFirstSortByRevisionDesc() {
    return dataAccessFeasibilityFormRepository.findAll(new Sort(Sort.Direction.DESC, "revision")).stream()
      .filter(form -> form.getRevision()>0)
      .findFirst();
  }

  private DataAccessFeasibilityForm createDefaultDataAccessFeasibilityForm() {
    DataAccessFeasibilityForm form = new DataAccessFeasibilityForm();
    form.setDefinition(getDefaultDataAccessFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDataAccessFormResourceAsString("schema.json"));
    form.setTitleFieldPath("projectTitle");
    form.setSummaryFieldPath("summary");
    form.setEndDateFieldPath("endDate");
    form.setRevision(0);
    return form;
  }
}
