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

import org.obiba.mica.micaConfig.domain.NetworkConfig;
import org.obiba.mica.micaConfig.repository.NetworkConfigRepository;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class NetworkConfigService extends EntityConfigService<NetworkConfig> {

  @Inject
  NetworkConfigRepository networkConfigRepository;

  @Override
  protected NetworkConfigRepository getRepository() {
    return networkConfigRepository;
  }

  @Override
  protected String getDefaultId() {
    return "default";
  }

  @Override
  protected NetworkConfig createEmptyForm() {
    return new NetworkConfig();
  }

  @Override
  protected String getDefaultDefinitionResourcePath() {
    return "classpath:config/network-form/definition.json";
  }

  @Override
  protected String getMandatoryDefinitionResourcePath() {
    return "classpath:config/network-form/definition-mandatory.json";
  }

  @Override
  protected String getDefaultSchemaResourcePath() {
    return "classpath:config/network-form/schema.json";
  }

  @Override
  protected String getMandatorySchemaResourcePath() {
    return "classpath:config/network-form/schema-mandatory.json";
  }
}
