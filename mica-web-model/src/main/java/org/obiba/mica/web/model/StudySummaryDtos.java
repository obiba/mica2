package org.obiba.mica.web.model;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.StudyState;

@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
class StudySummaryDtos {

  private StudySummaryDtos() {}

  @NotNull
  static Mica.StudySummaryDto asDto(@NotNull StudyState studyState) {
    Mica.StudySummaryDto.Builder builder = Mica.StudySummaryDto.newBuilder();
    builder.setId(studyState.getId()) //
        .setTimestamps(TimestampsDtos.asDto(studyState)) //
        .addAllName(LocalizedStringDtos.asDto(studyState.getName()));
    return builder.build();
  }

}
