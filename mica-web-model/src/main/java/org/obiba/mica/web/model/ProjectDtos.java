package org.obiba.mica.web.model;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.JSONUtils;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.domain.ProjectState;
import org.obiba.mica.project.service.ProjectService;
import org.springframework.stereotype.Component;

@Component
class ProjectDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private ProjectService projectService;

  @Inject
  private PermissionsDtos permissionsDtos;

  @Inject
  private EntityStateDtos entityStateDtos;

  @NotNull
  public Mica.ProjectDto asDto(@NotNull Project project, boolean asDraft) {
    Mica.ProjectDto.Builder builder = Mica.ProjectDto.newBuilder()
      .addAllName(localizedStringDtos.asDto(project.getName())) //
      .addAllDescription(localizedStringDtos.asDto(project.getDescription()));

    if(project.hasContent()) builder.setContent(JSONUtils.toJSON(project.getContent())); //
    if(!project.isNew()) builder.setId(project.getId());

    Mica.PermissionsDto permissionsDto = permissionsDtos.asDto(project);

    if(asDraft) {
      ProjectState projectState = projectService.getEntityState(project.getId());
      builder.setTimestamps(TimestampsDtos.asDto(project)) //
          .setPublished(projectState.isPublished()) //
          .setExtension(Mica.EntityStateDto.projectState,
              entityStateDtos.asDto(projectState).setPermissions(permissionsDto).build());
    }

    builder.setPermissions(permissionsDto);

    return builder.build();
  }

  @NotNull
  public Project fromDto(@NotNull Mica.ProjectDto dto) {
    Project.Builder builder = Project.newBuilder();
    builder.content(dto.hasContent() ? dto.getContent() : null);
    builder.name(localizedStringDtos.fromDto(dto.getNameList()));
    builder.description(localizedStringDtos.fromDto(dto.getDescriptionList()));

    Project project = builder.build();
    if(dto.hasId()) project.setId(dto.getId());

    TimestampsDtos.fromDto(dto.getTimestamps(), project);

    return project;
  }
}
