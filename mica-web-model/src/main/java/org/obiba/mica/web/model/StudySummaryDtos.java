package org.obiba.mica.web.model;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.StudyState;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
class StudySummaryDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @NotNull
  Mica.StudySummaryDto asDto(@NotNull StudyState studyState) {
    Mica.StudySummaryDto.Builder builder = Mica.StudySummaryDto.newBuilder();
    builder.setId(studyState.getId()) //
        .setTimestamps(TimestampsDtos.asDto(studyState)) //
        .addAllName(localizedStringDtos.asDto(studyState.getName())) //
        .setRevisionsAhead(studyState.getRevisionsAhead());
    if (studyState.isPublished()) builder.setPublishedTag(studyState.getPublishedTag());
    return builder.build();
  }

}
