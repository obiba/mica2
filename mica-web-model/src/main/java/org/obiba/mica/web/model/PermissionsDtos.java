/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
class PermissionsDtos {

  @Inject
  private SubjectAclService subjectAclService;

  private PermissionsDtos() {}

  public Mica.PermissionsDto asDto(@NotNull  Network network) {
    return asDto("/draft/network", network.getId());
  }

  public Mica.PermissionsDto asDto(@NotNull Study study) {
    return asDto("/draft/collection-study", study.getId());
  }

  public Mica.PermissionsDto asDto(@NotNull HarmonizationStudy study) {
    return asDto("/draft/harmonization-study", study.getId());
  }

  public Mica.PermissionsDto asDto(EntityState studyState) {
    return asDto("/draft/collection-study", studyState.getId());
  }

  public Mica.PermissionsDto asDto(StudyDataset  dataset) {
    return asDto("/draft/study-dataset", dataset.getId());
  }

  public Mica.PermissionsDto asDto(HarmonizationDataset dataset) {
    return asDto("/draft/harmonization-dataset", dataset.getId());
  }

  public Mica.PermissionsDto asDto(@NotNull AttachmentState state) {
    return asDto("/draft/file", state.getFullPath());
  }

  public Mica.PermissionsDto asDto(@NotNull Project project) {
    return asDto("/draft/project", project.getId());
  }

  public Mica.PermissionsDto asDto(@NotNull String resource, @Nullable String instance) {
    Mica.PermissionsDto.Builder builder = Mica.PermissionsDto.newBuilder();

    if (Strings.isNullOrEmpty(instance)) {
      builder.setAdd(subjectAclService.isPermitted(resource, "ADD"));
    } else {
      builder.setView(subjectAclService.isPermitted(resource, "VIEW", instance)) //
        .setEdit(subjectAclService.isPermitted(resource, "EDIT", instance)) //
        .setDelete(subjectAclService.isPermitted(resource, "DELETE", instance)) //
        .setPublish(subjectAclService.isPermitted(resource, "PUBLISH", instance));
    }

    return builder.build();
  }
}
