/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import javax.inject.Inject;
import javax.ws.rs.Path;

import org.obiba.mica.micaConfig.domain.DatasetConfig;
import org.obiba.mica.micaConfig.service.DatasetConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/config/" + DatasetConfigResource.TARGET_NAME)
public class DatasetConfigResource
  extends EntityConfigResource<DatasetConfig, Mica.EntityFormDto>
  implements PermissionAwareResource {

  static final String TARGET_NAME = "dataset";

  @Inject
  DatasetConfigService datasetConfigService;

  @Inject
  Dtos dtos;

  @Override
  protected Mica.EntityFormDto asDto(DatasetConfig datasetConfig) {
    return dtos.asDto(datasetConfig);
  }

  @Override
  protected DatasetConfig fromDto(Mica.EntityFormDto dto) {
    return dtos.fromDto(dto);
  }

  @Override
  protected DatasetConfigService getConfigService() {
    return datasetConfigService;
  }

  @Override
  public String getTarget() {
    return TARGET_NAME;
  }
}
