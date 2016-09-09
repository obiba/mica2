/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import org.obiba.mica.micaConfig.domain.ProjectConfig;
import org.obiba.mica.micaConfig.repository.ProjectConfigRepository;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class ProjectConfigService {

  @Inject
  GitService gitService;

  @Inject
  FileStoreService fileStoreService;

  @Inject
  ProjectConfigRepository projectConfigRepository;

  public void createOrUpdate(ProjectConfig projectConfig) {
    validateForm(projectConfig);
    projectConfig.incrementRevisionsAhead();
    gitService.save(projectConfig);
    projectConfigRepository.save(projectConfig);
  }

  public Optional<ProjectConfig> find() {
    ProjectConfig form = projectConfigRepository.findOne(ProjectConfig.DEFAULT_ID);
    if(form == null) {
      createOrUpdate(createDefaultProjectForm());
    }

    return Optional.ofNullable(form == null ? projectConfigRepository.findOne(ProjectConfig.DEFAULT_ID) : form);
  }

  public void publish() {
    Optional<ProjectConfig> projectForm = find();
    projectForm.ifPresent(d -> {
      d.setPublishedTag(gitService.tag(d).getFirst());
      d.setRevisionsAhead(0);
      d.setRevisionStatus(RevisionStatus.DRAFT);
      projectConfigRepository.save(d);
    });
  }

  private void validateForm(ProjectConfig projectConfig) {
    validateSchema(projectConfig.getSchema());
    validateDefinition(projectConfig.getDefinition());
  }

  private void validateSchema(String json) {
    try {
      new JSONObject(json);
    } catch(JSONException e) {
      throw new InvalidFormSchemaException(e);
    }
  }

  private void validateDefinition(String json) {
    try {
      new JSONArray(json);
    } catch(JSONException e) {
      throw new InvalidFormDefinitionException();
    }
  }

  private ProjectConfig createDefaultProjectForm() {
    ProjectConfig form = new ProjectConfig();
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
