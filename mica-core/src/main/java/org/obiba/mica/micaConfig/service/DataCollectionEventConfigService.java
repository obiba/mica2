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

import org.obiba.mica.micaConfig.domain.DataCollectionEventConfig;
import org.obiba.mica.micaConfig.repository.DataCollectionEventConfigRepository;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

@Component
public class DataCollectionEventConfigService extends EntityConfigService<DataCollectionEventConfig> {

  @Inject
  DataCollectionEventConfigRepository dataCollectionEventConfigRepository;

  @Override
  protected DataCollectionEventConfigRepository getRepository() {
    return dataCollectionEventConfigRepository;
  }

  @Override
  protected String getDefaultId() {
    return "default";
  }

  @Override
  protected DataCollectionEventConfig createEmptyForm() {
    return new DataCollectionEventConfig();
  }

  @Override
  protected String getDefaultDefinitionResourcePath() {
    return "classpath:config/data-collection-event-form/definition.json";
  }

  @Override
  protected String getMandatoryDefinitionResourcePath() {
    return "classpath:config/data-collection-event-form/definition-mandatory.json";
  }

  @Override
  protected String getDefaultSchemaResourcePath() {
    return "classpath:config/data-collection-event-form/schema.json";
  }

  @Override
  protected String getMandatorySchemaResourcePath() {
    return "classpath:config/data-collection-event-form/schema-mandatory.json";
  }
}
