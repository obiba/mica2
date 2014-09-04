package org.obiba.mica.web.model;

import java.util.Collection;
import java.util.HashSet;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.study.StudyService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
class StudySummaryDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private StudyService studyService;

  @NotNull
  Mica.StudySummaryDto asDto(@NotNull Study study) {
    return asDtoBuilder(study).build();
  }

  @NotNull
  Mica.StudySummaryDto asDto(@NotNull StudyState studyState) {
    Mica.StudyStateDto.Builder stateBuilder = Mica.StudyStateDto.newBuilder()
        .setRevisionsAhead(studyState.getRevisionsAhead());

    if(studyState.isPublished()) {
      stateBuilder.setPublishedTag(studyState.getPublishedTag());
    }

    Study study = studyState.isPublished()
        ? studyService.findPublishedStudy(studyState.getId())
        : studyService.findDraftStudy(studyState.getId());

    Mica.StudySummaryDto.Builder builder;
    if (study == null) {
      builder = Mica.StudySummaryDto.newBuilder();
      builder.setId(studyState.getId()) //
          .setTimestamps(TimestampsDtos.asDto(studyState)) //
          .addAllName(localizedStringDtos.asDto(studyState.getName()));
    } else {
      builder = asDtoBuilder(study);
    }

    return builder.setExtension(Mica.StudyStateDto.state, stateBuilder.build()).build();
  }

  private Mica.StudySummaryDto.Builder asDtoBuilder(@NotNull Study study) {
    Mica.StudySummaryDto.Builder builder = Mica.StudySummaryDto.newBuilder();

    builder.setId(study.getId()) //
        .setTimestamps(TimestampsDtos.asDto(study)) //
        .addAllName(localizedStringDtos.asDto(study.getName()));

    if(study.getAcronym() != null) {
      builder.addAllAcronym(localizedStringDtos.asDto(study.getAcronym()));
    }
    if(study.getMethods() != null && study.getMethods().getDesigns() != null) {
      builder.addAllDesigns(study.getMethods().getDesigns());
    }
    if(study.getNumberOfParticipants() != null && study.getNumberOfParticipants().getParticipant() != null) {
      builder.setTargetNumber(TargetNumberDtos.asDto(study.getNumberOfParticipants().getParticipant()));
    }
    Collection<String> countries = new HashSet<>();
    if(study.getPopulations() != null) {
      study.getPopulations().stream() //
          .filter(population -> population.getSelectionCriteria().getCountriesIso() != null)
          .forEach(population -> countries.addAll(population.getSelectionCriteria().getCountriesIso()));
    }
    builder.addAllCountries(countries);
    return builder;
  }

}
