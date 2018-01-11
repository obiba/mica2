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

import javax.inject.Inject;

import org.obiba.mica.micaConfig.domain.HarmonizationStudyConfig;
import org.obiba.mica.micaConfig.repository.HarmonizationStudyConfigRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

@Component
public class HarmonizationStudyConfigService extends EntityConfigService<HarmonizationStudyConfig> {

  @Inject
  HarmonizationStudyConfigRepository harmonizationConfigRepository;

  @Override
  protected MongoRepository<HarmonizationStudyConfig, String> getRepository() {
    return harmonizationConfigRepository;
  }

  @Override
  protected String getDefaultId() {
    return "default";
  }

  @Override
  protected HarmonizationStudyConfig createEmptyForm() {
    return new HarmonizationStudyConfig();
  }

  @Override
  protected String getDefaultSchemaResourcePath() {
    return "classpath:config/study-form/harmonization-schema.json";
  }

  @Override
  protected String getMandatorySchemaResourcePath() {
    return "classpath:config/study-form/schema-mandatory.json";
  }

  @Override
  protected String getDefaultDefinitionResourcePath() {
    return "classpath:config/study-form/harmonization-definition.json";
  }

  @Override
  protected String getMandatoryDefinitionResourcePath() {
    return "classpath:config/study-form/definition-mandatory.json";
  }
}
