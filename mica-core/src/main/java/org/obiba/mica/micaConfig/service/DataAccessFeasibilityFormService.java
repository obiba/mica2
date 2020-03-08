package org.obiba.mica.micaConfig.service;

import org.obiba.mica.micaConfig.domain.DataAccessFeasibilityForm;
import org.obiba.mica.micaConfig.repository.DataAccessFeasibilityFormRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
    return dataAccessFeasibilityFormRepository.save(dataAccessForm);
  }

  @Override
  public Optional<DataAccessFeasibilityForm> find() {
    DataAccessFeasibilityForm form = dataAccessFeasibilityFormRepository.findOne(DataAccessFeasibilityForm.DEFAULT_ID);

    if (form == null) {
      createOrUpdate(createDefaultDataAccessFeasibilityForm());
      form = dataAccessFeasibilityFormRepository.findOne(DataAccessFeasibilityForm.DEFAULT_ID);
    }

    if (StringUtils.isEmpty(form.getCsvExportFormat())) {
      form.setCsvExportFormat(getDefaultDataAccessFormResourceAsString("export-csv-schema.json"));
      form = createOrUpdate(form);
    }

    return Optional.ofNullable(form);
  }

  @Override
  String getDataAccessEntityFormResourceLocation() {
    return "classpath:config/data-access-feasibility-form/";
  }

  private DataAccessFeasibilityForm createDefaultDataAccessFeasibilityForm() {
    DataAccessFeasibilityForm form = new DataAccessFeasibilityForm();
    form.setDefinition(getDefaultDataAccessFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDataAccessFormResourceAsString("schema.json"));
    form.setCsvExportFormat(getDefaultDataAccessFormResourceAsString("export-csv-schema.json"));
    form.setTitleFieldPath("projectTitle");
    form.setSummaryFieldPath("summary");
    form.setEndDateFieldPath("endDate");
    return form;
  }
}
