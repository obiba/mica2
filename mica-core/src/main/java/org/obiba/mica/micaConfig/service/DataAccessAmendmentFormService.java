package org.obiba.mica.micaConfig.service;

import java.util.Optional;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.domain.DataAccessAmendmentForm;
import org.obiba.mica.micaConfig.repository.DataAccessAmendmentFormRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
    return dataAccessAmendmentFormRepository.save(dataAccessForm);
  }

  @Override
  public Optional<DataAccessAmendmentForm> find() {
    DataAccessAmendmentForm form = dataAccessAmendmentFormRepository.findOne(DataAccessAmendmentForm.DEFAULT_ID);

    if (form == null) {
      createOrUpdate(createDefaultDataAccessAmendmentForm());
      form = dataAccessAmendmentFormRepository.findOne(DataAccessAmendmentForm.DEFAULT_ID);
    }

    if (StringUtils.isEmpty(form.getCsvExportFormat())) {
      form.setCsvExportFormat(getDefaultDataAccessFormResourceAsString("export-csv-schema.json"));
      form = createOrUpdate(form);
    }

    return Optional.ofNullable(form);
  }

  @Override
  String getDataAccessEntityFormResourceLocation() {
    return "classpath:config/data-access-amendment-form/";
  }

  private DataAccessAmendmentForm createDefaultDataAccessAmendmentForm() {
    DataAccessAmendmentForm form = new DataAccessAmendmentForm();
    form.setDefinition(getDefaultDataAccessFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDataAccessFormResourceAsString("schema.json"));
    form.setCsvExportFormat(getDefaultDataAccessFormResourceAsString("export-csv-schema.json"));
    form.setTitleFieldPath("projectTitle");
    form.setSummaryFieldPath("summary");
    return form;
  }
}
