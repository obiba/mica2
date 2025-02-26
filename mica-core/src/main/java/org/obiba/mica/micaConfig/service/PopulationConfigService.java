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

import org.obiba.mica.micaConfig.domain.PopulationConfig;
import org.obiba.mica.micaConfig.repository.PopulationConfigRepository;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

@Component
public class PopulationConfigService extends EntityConfigService<PopulationConfig> {

  @Inject
  PopulationConfigRepository populationConfigRepository;

  @Override
  protected PopulationConfigRepository getRepository() {
    return populationConfigRepository;
  }

  @Override
  protected String getDefaultId() {
    return "default";
  }

  @Override
  protected PopulationConfig createEmptyForm() {
    return new PopulationConfig();
  }

  @Override
  protected String getDefaultDefinitionResourcePath() {
    return "classpath:config/population-form/definition.json";
  }

  @Override
  protected String getMandatoryDefinitionResourcePath() {
    return "classpath:config/population-form/definition-mandatory.json";
  }

  @Override
  protected String getDefaultSchemaResourcePath() {
    return "classpath:config/population-form/schema.json";
  }

  @Override
  protected String getMandatorySchemaResourcePath() {
    return "classpath:config/population-form/schema-mandatory.json";
  }
}
