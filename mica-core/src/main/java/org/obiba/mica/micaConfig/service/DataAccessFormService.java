package org.obiba.mica.micaConfig.service;

import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;

import javax.inject.Inject;

import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.repository.DataAccessFormRepository;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
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
    DataAccessForm form = dataAccessFormRepository.findOne(DataAccessForm.DEFAULT_ID);
    if(form == null) {
      createOrUpdateDataAccessForm(createDefaultDataAccessForm());
    }

    return Optional.ofNullable(form == null ? dataAccessFormRepository.findOne(DataAccessForm.DEFAULT_ID) : form);
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

  private DataAccessForm createDefaultDataAccessForm() {
    DataAccessForm form = new DataAccessForm();
    form.setDefinition(getDefaultDataAccessFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDataAccessFormResourceAsString("schema.json"));
    return form;
  }

  private Resource getDefaultDataAccessFormResource(String name) {
    return new DefaultResourceLoader().getResource("classpath:config/data-access-form/" + name);
  }

  private String getDefaultDataAccessFormResourceAsString(String name) {
    try(Scanner s = new Scanner(getDefaultDataAccessFormResource(name).getInputStream())) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    } catch(IOException e) {
      return "";
    }
  }
}
