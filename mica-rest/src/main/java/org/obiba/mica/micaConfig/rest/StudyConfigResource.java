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

import org.obiba.mica.micaConfig.domain.StudyConfig;
import org.obiba.mica.micaConfig.service.IndividualStudyConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Path;

@Component
@Path("/config/" + StudyConfigResource.TARGET_NAME)
public class StudyConfigResource
  extends EntityConfigResource<StudyConfig, Mica.EntityFormDto>
  implements PermissionAwareResource  {

  static final String TARGET_NAME = "individual-study";

  @Inject
  IndividualStudyConfigService studyConfigService;

  @Inject
  Dtos dtos;

  @Override
  protected Mica.EntityFormDto asDto(StudyConfig studyConfig) {
    return dtos.asDto(studyConfig);
  }

  @Override
  protected StudyConfig fromDto(Mica.EntityFormDto dto) {
    return dtos.fromDto(dto);
  }

  @Override
  protected IndividualStudyConfigService getConfigService() {
    return studyConfigService;
  }

  @Override
  public String getTarget() {
    return TARGET_NAME;
  }
}
