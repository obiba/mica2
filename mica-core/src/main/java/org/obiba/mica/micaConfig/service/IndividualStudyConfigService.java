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

import org.obiba.mica.micaConfig.domain.StudyConfig;
import org.obiba.mica.micaConfig.repository.StudyConfigRepository;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

@Component
public class IndividualStudyConfigService extends EntityConfigService<StudyConfig> {

  @Inject
  StudyConfigRepository studyConfigRepository;

  @Override
  protected StudyConfigRepository getRepository() {
    return studyConfigRepository;
  }

  @Override
  protected String getDefaultId() {
    return "default";
  }

  @Override
  protected StudyConfig createEmptyForm() {
    return new StudyConfig();
  }

  @Override
  protected String getDefaultSchemaResourcePath() {
    return "classpath:config/study-form/collection-schema.json";
  }

  @Override
  protected String getMandatorySchemaResourcePath() {
    return "classpath:config/study-form/schema-mandatory.json";
  }

  @Override
  protected String getDefaultDefinitionResourcePath() {
    return "classpath:config/study-form/collection-definition.json";
  }

  @Override
  protected String getMandatoryDefinitionResourcePath() {
    return "classpath:config/study-form/definition-mandatory.json";
  }
}
