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
import org.obiba.mica.study.service.PublishedDatasetVariableService;
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
  private PermissionsDtos permissionsDtos;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private PublishedDatasetVariableService datasetVariableService;

  @Inject
  private StudyService studyService;

  @NotNull
  public Mica.StudySummaryDto.Builder asDtoBuilder(@NotNull Study study) {
    StudyState studyState = studyService.getEntityState(study.getId());

    if(studyState.isPublished()) {
      return asDtoBuilder(study, studyState.isPublished(), datasetVariableService.getCountByStudyId(study.getId()));
    }

    return asDtoBuilder(study, studyState.isPublished(), 0);
  }

  @NotNull
  public Mica.StudySummaryDto.Builder asDtoBuilder(@NotNull Study study, boolean isPublished, long variablesCount) {
    Mica.StudySummaryDto.Builder builder = Mica.StudySummaryDto.newBuilder();
    builder.setPublished(isPublished);

    builder.setId(study.getId()) //
      .setTimestamps(TimestampsDtos.asDto(study)) //
      .addAllName(localizedStringDtos.asDto(study.getName())) //
      .addAllAcronym(localizedStringDtos.asDto(study.getAcronym())) //
      .addAllObjectives(localizedStringDtos.asDto(study.getObjectives()))
      .setVariables(isPublished ? variablesCount : 0);

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
        .filter(population -> population.getSelectionCriteria() != null &&
          population.getSelectionCriteria().getCountriesIso() != null)
        .forEach(population -> countries.addAll(population.getSelectionCriteria().getCountriesIso()));

      List<String> dataSources = Lists.newArrayList();
      populations.stream().filter(population -> population.getAllDataSources() != null)
        .forEach(population -> dataSources.addAll(population.getAllDataSources()));

      if(dataSources.size() > 0) {
        builder.addAllDataSources(dataSources.stream().distinct().collect(Collectors.toList()));
      }

      populations.forEach(population -> builder.addPopulationSummaries(asDto(population)));
    }

    builder.setPermissions(permissionsDtos.asDto(study));

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
  Mica.StudySummaryDto asDto(@NotNull Study study, @NotNull StudyState studyState) {
    Mica.StudyStateDto.Builder stateBuilder = Mica.StudyStateDto.newBuilder()
      .setRevisionsAhead(studyState.getRevisionsAhead()) //
      .setRevisionStatus(studyState.getRevisionStatus().name());

    if(studyState.isPublished()) {
      stateBuilder.setPublishedTag(studyState.getPublishedTag());
      if(studyState.hasPublishedId()) {
        stateBuilder.setPublishedId(studyState.getPublishedId());
      }
      if(studyState.hasPublicationDate()) {
        stateBuilder.setPublicationDate(studyState.getPublicationDate().toString());
        stateBuilder.setPublishedBy(studyState.getPublishedBy());
      }
    }

    stateBuilder.setPermissions(permissionsDtos.asDto(studyState));

    Mica.StudySummaryDto.Builder builder;

    if(study == null) {
      builder = Mica.StudySummaryDto.newBuilder();
      builder.setId(studyState.getId()) //
        .setTimestamps(TimestampsDtos.asDto(studyState)) //
        .addAllName(localizedStringDtos.asDto(studyState.getName()));
    } else {
      builder = asDtoBuilder(study, studyState.isPublished(), 0);
    }

    builder.setPublished(studyState.isPublished());

    return builder.setExtension(Mica.StudyStateDto.state, stateBuilder.build()).build();
  }

  @NotNull
  Mica.StudySummaryDto asDto(@NotNull StudyState studyState) {
    Study study = studyService.findStudy(studyState.getId());
    return asDto(study, studyState);
  }

  Mica.StudySummaryDto asDto(String studyId) {
    StudyState studyState = studyService.getEntityState(studyId);

    if(studyState.isPublished()) {
      Study study = publishedStudyService.findById(studyId);
      if(study != null) return asDtoBuilder(study, true, datasetVariableService.getCountByStudyId(studyId)).build();
    }

    return asDto(studyState);
  }
}
