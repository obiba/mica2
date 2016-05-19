package org.obiba.mica.web.model;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.JSONUtils;
import org.obiba.mica.project.domain.Project;
import org.springframework.stereotype.Component;

@Component
class ProjectDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @NotNull
  public Mica.ProjectDto asDto(@NotNull Project project, boolean asDraft) {
    Mica.ProjectDto.Builder builder = Mica.ProjectDto.newBuilder();
    builder.addAllTitle(localizedStringDtos.asDto(project.getName())) //
      .setTimestamps(TimestampsDtos.asDto(project)); //
    if(project.hasContent()) builder.setContent(JSONUtils.toJSON(project.getContent())); //
    if(!project.isNew()) builder.setId(project.getId());

    return builder.build();
  }

  @NotNull
  public Project fromDto(@NotNull Mica.ProjectDto dto) {
    Project.Builder builder = Project.newBuilder();
    builder.content(dto.hasContent() ? dto.getContent() : null);

    Project project = builder.build();
    if(dto.hasId()) project.setId(dto.getId());

    TimestampsDtos.fromDto(dto.getTimestamps(), project);

    return project;
  }
}
