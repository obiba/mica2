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

import org.obiba.mica.micaConfig.domain.HarmonizationDatasetConfig;
import org.obiba.mica.micaConfig.repository.HarmonizationDatasetConfigRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

@Component
public class HarmonizationDatasetConfigService extends EntityConfigService<HarmonizationDatasetConfig> {

  @Inject
  HarmonizationDatasetConfigRepository datasetConfigRepository;

  @Override
  protected MongoRepository<HarmonizationDatasetConfig, String> getRepository() {
    return datasetConfigRepository;
  }

  @Override
  protected String getDefaultId() {
    return "default";
  }

  @Override
  protected HarmonizationDatasetConfig createEmptyForm() {
    return new HarmonizationDatasetConfig();
  }

  @Override
  protected String getDefaultDefinitionResourcePath() {
    return "classpath:config/dataset-form/harmonization-definition.json";
  }

  @Override
  protected String getMandatoryDefinitionResourcePath() {
    return "classpath:config/dataset-form/definition-mandatory.json";
  }

  @Override
  protected String getDefaultSchemaResourcePath() {
    return "classpath:config/dataset-form/harmonization-schema.json";
  }

  @Override
  protected String getMandatorySchemaResourcePath() {
    return "classpath:config/dataset-form/schema-mandatory.json";
  }
}
