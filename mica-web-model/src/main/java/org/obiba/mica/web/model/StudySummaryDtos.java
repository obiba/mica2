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
  Mica.StudySummaryDto asDto(@NotNull StudyState studyState) {

    Mica.StudySummaryDto.Builder builder = Mica.StudySummaryDto.newBuilder();
    builder.setId(studyState.getId()) //
        .setTimestamps(TimestampsDtos.asDto(studyState)) //
        .addAllName(localizedStringDtos.asDto(studyState.getName())) //
        .setRevisionsAhead(studyState.getRevisionsAhead());
    if(studyState.isPublished()) {
      builder.setPublishedTag(studyState.getPublishedTag());
    }
    Study study = studyState.isPublished()
        ? studyService.findPublishedStudy(studyState.getId())
        : studyService.findDraftStudy(studyState.getId());

    if (study == null) return builder.build();

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
    return builder.build();
  }

}
