package org.obiba.mica.micaConfig.service;

import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;

import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.file.FileStoreService;
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
  FileStoreService fileStoreService;

  @Inject
  DataAccessFormRepository dataAccessFormRepository;

  public void createOrUpdateDataAccessForm(DataAccessForm dataAccessForm) {
    validateForm(dataAccessForm);
    dataAccessForm.incrementRevisionsAhead();
    gitService.save(dataAccessForm);

    dataAccessForm.getPdfTemplates().forEach((k,v)-> {
      if(v.isJustUploaded()) {
        fileStoreService.save(v.getId());
        v.setJustUploaded(false);
      }
    });

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
      d.setPublishedTag(gitService.tag(d).getFirst());
      d.setRevisionsAhead(0);
      d.setRevisionStatus(RevisionStatus.DRAFT);
      dataAccessFormRepository.save(d);
    });
  }

  private void validateForm(DataAccessForm dataAccessForm) {
    validateSchema(dataAccessForm.getSchema());
    validateDefinition(dataAccessForm.getDefinition());
  }

  private void validateSchema(String json) {
    try {
      new JSONObject(json);
    } catch(JSONException e) {
      throw new InvalidFormSchemaException();
    }
  }

  private void validateDefinition(String json) {
    try {
      new JSONArray(json);
    } catch(JSONException e) {
      throw new InvalidFormDefinitionException();
    }
  }

  private DataAccessForm createDefaultDataAccessForm() {
    DataAccessForm form = new DataAccessForm();
    form.setDefinition(getDefaultDataAccessFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDataAccessFormResourceAsString("schema.json"));
    form.setTitleFieldPath("projectTitle");
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
