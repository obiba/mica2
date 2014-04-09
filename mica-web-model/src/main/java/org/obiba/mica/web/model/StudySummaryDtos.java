package org.obiba.mica.web.model;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.StudyState;
import org.obiba.mica.domain.Timestamped;

@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
class StudySummaryDtos {

  private StudySummaryDtos() {}

  @NotNull
  static Mica.StudySummaryDto asDto(@NotNull StudyState studyState) {
    Mica.StudySummaryDto.Builder builder = Mica.StudySummaryDto.newBuilder();
    builder.setId(studyState.getId()) //
        .setTimestamps(TimestampsDtos.asDto((Timestamped) studyState)) //
        .addAllName(LocalizedStringDtos.asDtos(studyState.getName()));
    return builder.build();
  }

}
