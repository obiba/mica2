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
import org.obiba.mica.micaConfig.domain.ProjectForm;
import org.obiba.mica.micaConfig.repository.ProjectFormRepository;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class ProjectFormService {

  @Inject
  GitService gitService;

  @Inject
  FileStoreService fileStoreService;

  @Inject
  ProjectFormRepository projectFormRepository;

  public void createOrUpdate(ProjectForm projectForm) {
    validateForm(projectForm);
    projectForm.incrementRevisionsAhead();
    gitService.save(projectForm);
    projectFormRepository.save(projectForm);
  }

  public Optional<ProjectForm> find() {
    ProjectForm form = projectFormRepository.findOne(ProjectForm.DEFAULT_ID);
    if(form == null) {
      createOrUpdate(createDefaultProjectForm());
    }

    return Optional.ofNullable(form == null ? projectFormRepository.findOne(ProjectForm.DEFAULT_ID) : form);
  }

  public void publish() {
    Optional<ProjectForm> projectForm = find();
    projectForm.ifPresent(d -> {
      d.setPublishedTag(gitService.tag(d).getFirst());
      d.setRevisionsAhead(0);
      d.setRevisionStatus(RevisionStatus.DRAFT);
      projectFormRepository.save(d);
    });
  }

  private void validateForm(ProjectForm projectForm) {
    validateSchema(projectForm.getSchema());
    validateDefinition(projectForm.getDefinition());
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

  private ProjectForm createDefaultProjectForm() {
    ProjectForm form = new ProjectForm();
    form.setDefinition(getDefaultProjectFormResourceAsString("definition.json"));
    form.setSchema(getDefaultProjectFormResourceAsString("schema.json"));
    return form;
  }

  private Resource getDefaultProjectFormResource(String name) {
    return new DefaultResourceLoader().getResource("classpath:config/project-form/" + name);
  }

  private String getDefaultProjectFormResourceAsString(String name) {
    try(Scanner s = new Scanner(getDefaultProjectFormResource(name).getInputStream())) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    } catch(IOException e) {
      return "";
    }
  }
}
