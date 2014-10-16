package org.obiba.mica.web.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Component
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
class StudySummaryDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttachmentDtos attachmentDtos;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private StudyService studyService;

  @NotNull
  public Mica.StudySummaryDto.Builder asDtoBuilder(@NotNull Study study) {
    Mica.StudySummaryDto.Builder builder = Mica.StudySummaryDto.newBuilder();

    builder.setId(study.getId()) //
        .setTimestamps(TimestampsDtos.asDto(study)) //
        .addAllName(localizedStringDtos.asDto(study.getName())) //
        .addAllAcronym(localizedStringDtos.asDto(study.getAcronym())) //
        .addAllObjectives(localizedStringDtos.asDto(study.getObjectives()));

    if(study.getLogo() != null) builder.setLogo(attachmentDtos.asDto(study.getLogo()));

    if(study.getMethods() != null && study.getMethods().getDesigns() != null) {
      builder.addAllDesigns(study.getMethods().getDesigns());
    }
    if(study.getNumberOfParticipants() != null && study.getNumberOfParticipants().getParticipant() != null) {
      builder.setTargetNumber(TargetNumberDtos.asDto(study.getNumberOfParticipants().getParticipant()));
    }
    Collection<String> countries = new HashSet<>();
    SortedSet<Population> populations = study.getPopulations();
    if(populations != null) {
      populations.stream() //
          .filter(population -> population.getSelectionCriteria().getCountriesIso() != null)
          .forEach(population -> countries.addAll(population.getSelectionCriteria().getCountriesIso()));

      List<String> dataSources = Lists.newArrayList();
      populations.stream().filter(population -> population.getAllDataSources() != null)
          .forEach(population -> dataSources.addAll(population.getAllDataSources()));

      if(dataSources.size() > 0) {
        builder.addAllDataSources(dataSources.stream().distinct().collect(Collectors.toList()));
      }

      populations.forEach(population -> builder.addPopulationSummaries(asDto(population)));
    }
    builder.addAllCountries(countries);
    return builder;
  }

  @NotNull
  Mica.PopulationSummaryDto asDto(@NotNull Population population) {
    Mica.PopulationSummaryDto.Builder builder = Mica.PopulationSummaryDto.newBuilder();

    builder.setId(population.getId()) //
        .addAllName(localizedStringDtos.asDto(population.getName()));

    if(population.getDataCollectionEvents() != null) {
      population.getDataCollectionEvents().forEach(dce -> builder.addDataCollectionEventSummaries(asDto(dce)));
    }

    return builder.build();
  }

  @NotNull
  Mica.DataCollectionEventSummaryDto asDto(@NotNull DataCollectionEvent dce) {
    return Mica.DataCollectionEventSummaryDto.newBuilder().setId(dce.getId()) //
        .addAllName(localizedStringDtos.asDto(dce.getName())).build();
  }

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
        ? publishedStudyService.findById(studyState.getId())
        : studyService.findDraftStudy(studyState.getId());

    Mica.StudySummaryDto.Builder builder;
    if(study == null) {
      builder = Mica.StudySummaryDto.newBuilder();
      builder.setId(studyState.getId()) //
          .setTimestamps(TimestampsDtos.asDto(studyState)) //
          .addAllName(localizedStringDtos.asDto(studyState.getName()));
    } else {
      builder = asDtoBuilder(study);
    }

    return builder.setExtension(Mica.StudyStateDto.state, stateBuilder.build()).build();
  }

  Mica.StudySummaryDto asDto(String studyId) {
    StudyState studyState = studyService.findStateById(studyId);
    if(studyState.isPublished()) {
      return asDto(publishedStudyService.findById(studyId));
    }

    return asDto(studyState);
  }

}
