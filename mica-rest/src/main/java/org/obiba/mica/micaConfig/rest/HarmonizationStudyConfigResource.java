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

import javax.inject.Inject;
import jakarta.ws.rs.Path;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.micaConfig.domain.HarmonizationStudyConfig;
import org.obiba.mica.micaConfig.service.EntityConfigService;
import org.obiba.mica.micaConfig.service.HarmonizationStudyConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/config/" + HarmonizationStudyConfigResource.TARGET_NAME)
@RequiresAuthentication
public class HarmonizationStudyConfigResource
  extends EntityConfigResource<HarmonizationStudyConfig, Mica.EntityFormDto>
  implements PermissionAwareResource {

  static final String TARGET_NAME = "harmonization-study";

  @Inject
  HarmonizationStudyConfigService harmonizationStudyConfigService;

  @Inject
  Dtos dtos;

  @Override
  protected Mica.EntityFormDto asDto(HarmonizationStudyConfig entityConfig) {
    return dtos.asDto(entityConfig);
  }

  @Override
  protected HarmonizationStudyConfig fromDto(Mica.EntityFormDto entityConfig) {
    return dtos.fromDto(entityConfig);
  }

  @Override
  protected EntityConfigService<HarmonizationStudyConfig> getConfigService() {
    return harmonizationStudyConfigService;
  }

  @Override
  public String getTarget() {
    return TARGET_NAME;
  }
}
