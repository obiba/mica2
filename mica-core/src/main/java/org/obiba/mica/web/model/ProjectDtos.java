/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.util.NoSuchElementException;

import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.JSONUtils;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.domain.ProjectState;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.stereotype.Component;

@Component
class ProjectDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private ProjectService projectService;

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private PermissionsDtos permissionsDtos;

  @Inject
  private EntityStateDtos entityStateDtos;

  @NotNull
  public Mica.ProjectDto asDto(@NotNull Project project, boolean asDraft) {
    Mica.ProjectDto.Builder builder = Mica.ProjectDto.newBuilder()
      .addAllTitle(localizedStringDtos.asDto(project.getTitle())) //
      .addAllSummary(localizedStringDtos.asDto(project.getSummary()));

    if(project.hasModel()) builder.setContent(JSONUtils.toJSON(project.getModel()));
    if(project.hasDataAccessRequestId()) {
      try {
        DataAccessRequest request = dataAccessRequestService.findById(project.getDataAccessRequestId());
        Mica.DataAccessRequestSummaryDto.Builder darBuilder = Mica.DataAccessRequestSummaryDto.newBuilder();
        darBuilder.setId(project.getDataAccessRequestId());
        darBuilder.setStatus(request.getStatus().name());
        darBuilder.setApplicant(request.getApplicant());
        darBuilder.setViewable(subjectAclService.isPermitted("/data-access-request", "VIEW", request.getId()));
        builder.setRequest(darBuilder);
      } catch(NoSuchElementException e) {
        // ignore
      }
    }
    if(!project.isNew()) builder.setId(project.getId());

    Mica.PermissionsDto permissionsDto = permissionsDtos.asDto(project);

    if(asDraft) {
      ProjectState projectState = projectService.getEntityState(project.getId());
      builder.setTimestamps(TimestampsDtos.asDto(project)) //
          .setPublished(projectState.isPublished()) //
        .setState(entityStateDtos.asDto(projectState).setPermissions(permissionsDto).build());
    }

    builder.setPermissions(permissionsDto);

    builder.setTimestamps(TimestampsDtos.asDto(project));

    return builder.build();
  }

  @NotNull
  public Project fromDto(@NotNull Mica.ProjectDto dto) {
    Project.Builder builder = Project.newBuilder();
    builder.content(dto.hasContent() ? dto.getContent() : null);
    builder.title(localizedStringDtos.fromDto(dto.getTitleList()));
    builder.summary(localizedStringDtos.fromDto(dto.getSummaryList()));

    Project project = builder.build();
    if(dto.hasId()) project.setId(dto.getId());

    TimestampsDtos.fromDto(dto.getTimestamps(), project);

    return project;
  }
}
