package org.obiba.mica.web.model;

import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Attachment;
import org.obiba.mica.domain.DataCollectionEvent;
import org.obiba.mica.domain.Population;

import static org.obiba.mica.web.model.Mica.StudyDto.PopulationDto;

class PopulationDtos {
  private PopulationDtos() {}

  @NotNull
  static PopulationDto asDto(Population population) {
    PopulationDto.Builder builder = PopulationDto.newBuilder();
    builder.setId(population.getId());
    if(population.getName() != null) builder.addAllName(LocalizedStringDtos.asDto(population.getName()));
    if(population.getDescription() != null) {
      builder.addAllDescription(LocalizedStringDtos.asDto(population.getDescription()));
    }
    if(population.getRecruitment() != null) builder.setRecruitment(asDto(population.getRecruitment()));
    if(population.getSelectionCriteria() != null) {
      builder.setSelectionCriteria(asDto(population.getSelectionCriteria()));
    }
    if(population.getNumberOfParticipants() != null) {
      builder.setNumberOfParticipants(NumberOfParticipantsDtos.asDto(population.getNumberOfParticipants()));
    }
    population.getDataCollectionEvents().forEach(dce -> builder.addDataCollectionEvents(asDto(dce)));

    return builder.build();
  }

  @NotNull
  static Population fromDto(PopulationDto dto) {
    Population population = new Population();
    population.setId(dto.getId());
    if(dto.getNameCount() > 0) population.setName(LocalizedStringDtos.fromDto(dto.getNameList()));
    if(dto.getDescriptionCount() > 0) population.setDescription(LocalizedStringDtos.fromDto(dto.getDescriptionList()));
    if(dto.hasRecruitment()) population.setRecruitment(fromDto(dto.getRecruitment()));
    if(dto.hasSelectionCriteria()) population.setSelectionCriteria(fromDto(dto.getSelectionCriteria()));
    if(dto.hasNumberOfParticipants()) {
      population.setNumberOfParticipants(NumberOfParticipantsDtos.fromDto(dto.getNumberOfParticipants()));
    }
    if(dto.getDataCollectionEventsCount() > 0) {
      dto.getDataCollectionEventsList()
          .forEach(dceDto -> population.getDataCollectionEvents().add(fromDto(dceDto)));
    }
    return population;
  }

  @NotNull
  private static PopulationDto.SelectionCriteriaDto asDto(Population.SelectionCriteria selectionCriteria) {
    PopulationDto.SelectionCriteriaDto.Builder builder = PopulationDto.SelectionCriteriaDto.newBuilder();
    builder.setGender(PopulationDto.SelectionCriteriaDto.Gender.valueOf(selectionCriteria.getGender().ordinal()));
    builder.setAgeMin(selectionCriteria.getAgeMin());
    builder.setAgeMax(selectionCriteria.getAgeMax());
    selectionCriteria.getCountriesIso().forEach(country -> builder.addCountriesIso(country));
    if(selectionCriteria.getTerritory() != null) {
      builder.addAllTerritory(LocalizedStringDtos.asDto(selectionCriteria.getTerritory()));
    }
    selectionCriteria.getCriteria().forEach(criteria -> builder.addCriteria(criteria));
    if(selectionCriteria.getEthnicOrigin().size() > 0) {
      builder.addAllEthnicOrigin(LocalizedStringDtos.asDtoList(selectionCriteria.getEthnicOrigin()));
    }
    if(selectionCriteria.getHealthStatus().size() > 0) {
      builder.addAllHealthStatus(LocalizedStringDtos.asDtoList(selectionCriteria.getHealthStatus()));
    }
    if(selectionCriteria.getOtherCriteria() != null) {
      builder.addAllOtherCriteria(LocalizedStringDtos.asDto(selectionCriteria.getOtherCriteria()));
    }
    if(selectionCriteria.getInfo() != null) builder.addAllInfo(LocalizedStringDtos.asDto(selectionCriteria.getInfo()));
    return builder.build();
  }

  @NotNull
  private static Population.SelectionCriteria fromDto(PopulationDto.SelectionCriteriaDto dto) {
    Population.SelectionCriteria selectionCriteria = new Population.SelectionCriteria();
    if(dto.hasGender()) {
      selectionCriteria.setGender(Population.SelectionCriteria.Gender.valueOf(dto.getGender().name()));
    }
    if(dto.hasAgeMin()) selectionCriteria.setAgeMin(dto.getAgeMin());
    if(dto.hasAgeMax()) selectionCriteria.setAgeMax(dto.getAgeMax());
    if(dto.getCountriesIsoCount() > 0) selectionCriteria.setCountriesIso(dto.getCountriesIsoList());
    if(dto.getTerritoryCount() > 0) selectionCriteria.setTerritory(LocalizedStringDtos.fromDto(dto.getTerritoryList()));
    if(dto.getCriteriaCount() > 0) selectionCriteria.setCriteria(dto.getCriteriaList());
    if(dto.getEthnicOriginCount() > 0) {
      selectionCriteria.setEthnicOrigin(LocalizedStringDtos.fromDtoList(dto.getEthnicOriginList()));
    }
    if(dto.getHealthStatusCount() > 0) {
      selectionCriteria.setHealthStatus(LocalizedStringDtos.fromDtoList(dto.getHealthStatusList()));
    }
    if(dto.getOtherCriteriaCount() > 0) {
      selectionCriteria.setOtherCriteria(LocalizedStringDtos.fromDto(dto.getOtherCriteriaList()));
    }
    if(dto.getInfoCount() > 0) selectionCriteria.setInfo(LocalizedStringDtos.fromDto(dto.getInfoList()));
    return selectionCriteria;
  }

  @NotNull
  private static PopulationDto.RecruitmentDto asDto(@NotNull Population.Recruitment recruitment) {
    PopulationDto.RecruitmentDto.Builder builder = PopulationDto.RecruitmentDto.newBuilder();
    recruitment.getDataSources().forEach(datasource -> builder.addDataSources(datasource));
    recruitment.getGeneralPopulationSources().forEach(datasource -> builder.addGeneralPopulationSources(datasource));
    recruitment.getSpecificPopulationSources().forEach(datasource -> builder.addSpecificPopulationSources(datasource));
    if(recruitment.getOtherSpecificPopulationSource() != null) {
      builder.addAllOtherSpecificPopulationSource(
          LocalizedStringDtos.asDto(recruitment.getOtherSpecificPopulationSource()));
    }
    builder.addAllStudies(LocalizedStringDtos.asDtoList(recruitment.getStudies()));
    if(recruitment.getOtherSource() != null) {
      builder.addAllOtherSource(LocalizedStringDtos.asDto(recruitment.getOtherSource()));
    }
    if(recruitment.getInfo() != null) builder.addAllInfo(LocalizedStringDtos.asDto(recruitment.getInfo()));
    return builder.build();
  }

  @NotNull
  private static Population.Recruitment fromDto(@NotNull PopulationDto.RecruitmentDto dto) {
    Population.Recruitment recruitment = new Population.Recruitment();
    if(dto.getDataSourcesCount() > 0) recruitment.setDataSources(dto.getDataSourcesList());
    if(dto.getGeneralPopulationSourcesCount() > 0) {
      recruitment.setGeneralPopulationSources(dto.getGeneralPopulationSourcesList());
    }
    if(dto.getSpecificPopulationSourcesCount() > 0) {
      recruitment.setSpecificPopulationSources(dto.getSpecificPopulationSourcesList());
    }
    if(dto.getOtherSpecificPopulationSourceCount() > 0) {
      recruitment
          .setOtherSpecificPopulationSource(LocalizedStringDtos.fromDto(dto.getOtherSpecificPopulationSourceList()));
    }
    recruitment.setStudies(LocalizedStringDtos.fromDtoList(dto.getStudiesList()));
    if(dto.getOtherSourceCount() > 0) {
      recruitment.setOtherSource(LocalizedStringDtos.fromDto(dto.getOtherSourceList()));
    }
    if(dto.getInfoCount() > 0) recruitment.setInfo(LocalizedStringDtos.fromDto(dto.getInfoList()));

    return recruitment;
  }

  @NotNull
  static PopulationDto.DataCollectionEventDto asDto(@NotNull DataCollectionEvent dce) {
    PopulationDto.DataCollectionEventDto.Builder builder = PopulationDto.DataCollectionEventDto.newBuilder();
    builder.setId(dce.getId());
    if(dce.getName() != null) builder.addAllName(LocalizedStringDtos.asDto(dce.getName()));
    if(dce.getDescription() != null) builder.addAllDescription(LocalizedStringDtos.asDto(dce.getDescription()));
    if(dce.getStartYear() != null) builder.setStartYear(dce.getStartYear());
    if(dce.getStartMonth() != null) builder.setStartMonth(dce.getStartMonth());
    if(dce.getEndYear() != null) builder.setEndYear(dce.getEndYear());
    if(dce.getEndMonth() != null) builder.setEndMonth(dce.getEndMonth());
    dce.getDataSources().forEach(ds -> builder.addDataSources(ds));
    dce.getAdministrativeDatabases().forEach(adminDb -> builder.addAdministrativeDatabases(adminDb));
    if(dce.getOtherDataSources() != null) {
      builder.addAllOtherDataSources(LocalizedStringDtos.asDto(dce.getOtherDataSources()));
    }
    dce.getBioSamples().forEach(sample -> builder.addBioSamples(sample));
    if(dce.getTissueTypes() != null) builder.addAllTissueTypes(LocalizedStringDtos.asDto(dce.getTissueTypes()));
    if(dce.getOtherBioSamples() != null) {
      builder.addAllOtherBioSamples(LocalizedStringDtos.asDto(dce.getOtherBioSamples()));
    }
    dce.getAttachments().forEach(attachment -> builder.addAttachments(AttachmentDtos.asDto(attachment)));
    return builder.build();
  }

  @NotNull
  static DataCollectionEvent fromDto(@NotNull PopulationDto.DataCollectionEventDto dto) {
    DataCollectionEvent dce = new DataCollectionEvent();
    dce.setId(dto.getId());
    if(dto.getNameCount() > 0) dce.setName(LocalizedStringDtos.fromDto(dto.getNameList()));
    if(dto.getDescriptionCount() > 0) dce.setDescription(LocalizedStringDtos.fromDto(dto.getDescriptionList()));
    if(dto.hasStartYear()) dce.setStartYear(dto.getStartYear());
    if(dto.hasStartMonth()) dce.setStartMonth(dto.getStartMonth());
    if(dto.hasEndYear()) dce.setEndYear(dto.getEndYear());
    if(dto.hasEndMonth()) dce.setEndMonth(dto.getEndMonth());
    if(dto.getDataSourcesCount() > 0) dce.setDataSources(dto.getDataSourcesList());
    if(dto.getAdministrativeDatabasesCount() > 0) dce.setAdministrativeDatabases(dto.getAdministrativeDatabasesList());
    if(dto.getOtherDataSourcesCount() > 0) {
      dce.setOtherDataSources(LocalizedStringDtos.fromDto(dto.getOtherDataSourcesList()));
    }
    if(dto.getBioSamplesCount() > 0) dce.setBioSamples(dto.getBioSamplesList());
    if(dto.getTissueTypesCount() > 0) dce.setTissueTypes(LocalizedStringDtos.fromDto(dto.getTissueTypesList()));
    if(dto.getOtherBioSamplesCount() > 0) {
      dce.setOtherBioSamples(LocalizedStringDtos.fromDto(dto.getOtherBioSamplesList()));
    }
    dce.setAttachments(
        dto.getAttachmentsList().stream().map(AttachmentDtos::fromDto).collect(Collectors.<Attachment>toList()));
    return dce;
  }
}
