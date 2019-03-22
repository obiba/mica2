/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.micaConfig.domain.HarmonizationDatasetConfig;
import org.obiba.mica.micaConfig.service.HarmonizationDatasetConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Path;

@Component
@Path("/config/" + HarmonizationDatasetConfigResource.TARGET_NAME)
@RequiresAuthentication
public class HarmonizationDatasetConfigResource
  extends EntityConfigResource<HarmonizationDatasetConfig, Mica.EntityFormDto>
  implements PermissionAwareResource {

  static final String TARGET_NAME = "harmonized-dataset";

  @Inject
  HarmonizationDatasetConfigService datasetConfigService;

  @Inject
  Dtos dtos;

  @Override
  protected Mica.EntityFormDto asDto(HarmonizationDatasetConfig datasetConfig) {
    return dtos.asDto(datasetConfig);
  }

  @Override
  protected HarmonizationDatasetConfig fromDto(Mica.EntityFormDto dto) {
    return dtos.fromDto(dto);
  }

  @Override
  protected HarmonizationDatasetConfigService getConfigService() {
    return datasetConfigService;
  }

  @Override
  public String getTarget() {
    return TARGET_NAME;
  }
}
