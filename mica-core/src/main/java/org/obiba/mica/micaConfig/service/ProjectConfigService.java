/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import org.obiba.mica.core.service.GitService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.ProjectConfig;
import org.obiba.mica.micaConfig.repository.ProjectConfigRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

@Component
public class ProjectConfigService extends EntityConfigService<ProjectConfig> {

  @Inject
  GitService gitService;

  @Inject
  FileStoreService fileStoreService;

  @Inject
  ProjectConfigRepository projectConfigRepository;

  @Override
  protected MongoRepository<ProjectConfig, String> getRepository() {
    return projectConfigRepository;
  }

  @Override
  protected String getDefaultId() {
    return ProjectConfig.DEFAULT_ID;
  }

  @Override
  protected ProjectConfig createEmptyForm() {
    return new ProjectConfig();
  }

  @Override
  protected String getDefaultSchemaResourcePath() {
    return "classpath:config/project-form/schema.json";
  }

  @Override
  protected String getMandatorySchemaResourcePath() {
    return "classpath:config/project-form/schema-mandatory.json";
  }

  @Override
  protected String getDefaultDefinitionResourcePath() {
    return "classpath:config/project-form/definition.json";
  }

  @Override
  protected String getMandatoryDefinitionResourcePath() {
    return "classpath:config/project-form/definition-mandatory.json";
  }
}
