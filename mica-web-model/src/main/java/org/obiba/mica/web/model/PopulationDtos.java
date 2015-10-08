package org.obiba.mica.web.model;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.study.date.PersistableYearMonth;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.springframework.stereotype.Component;

import static org.obiba.mica.web.model.Mica.StudyDto.PopulationDto;

@Component
@SuppressWarnings({ "OverlyLongMethod", "OverlyCoupledClass" })
class PopulationDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private NumberOfParticipantsDtos numberOfParticipantsDtos;

  @Inject
  private AttachmentDtos attachmentDtos;

  @Inject
  private AttributeDtos attributeDtos;

  @NotNull
  PopulationDto asDto(Population population) {
    PopulationDto.Builder builder = PopulationDto.newBuilder();
    builder.setId(population.getId());
    if(population.getName() != null) builder.addAllName(localizedStringDtos.asDto(population.getName()));
    if(population.getDescription() != null) {
      builder.addAllDescription(localizedStringDtos.asDto(population.getDescription()));
    }
    if(population.getRecruitment() != null) builder.setRecruitment(asDto(population.getRecruitment()));
    if(population.getSelectionCriteria() != null) {
      builder.setSelectionCriteria(asDto(population.getSelectionCriteria()));
    }
    if(population.getNumberOfParticipants() != null) {
      builder.setNumberOfParticipants(numberOfParticipantsDtos.asDto(population.getNumberOfParticipants()));
    }
    if(population.getDataCollectionEvents() != null) {
      population.getDataCollectionEvents().forEach(dce -> builder.addDataCollectionEvents(asDto(dce)));
    }
    if(population.getAttributes() != null) {
      population.getAttributes().asAttributeList().forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }
    return builder.build();
  }

  @NotNull
  Population fromDto(Mica.StudyDto.PopulationDtoOrBuilder dto) {
    Population population = new Population();
    population.setId(dto.getId());
    if(dto.getNameCount() > 0) population.setName(localizedStringDtos.fromDto(dto.getNameList()));
    if(dto.getDescriptionCount() > 0) population.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    if(dto.hasRecruitment()) population.setRecruitment(fromDto(dto.getRecruitment()));
    if(dto.hasSelectionCriteria()) population.setSelectionCriteria(fromDto(dto.getSelectionCriteria()));
    if(dto.hasNumberOfParticipants()) {
      population.setNumberOfParticipants(numberOfParticipantsDtos.fromDto(dto.getNumberOfParticipants()));
    }
    if(dto.getDataCollectionEventsCount() > 0) {
      dto.getDataCollectionEventsList().forEach(dceDto -> population.addDataCollectionEvent(fromDto(dceDto)));
    }
    if(dto.getAttributesCount() > 0) {
      dto.getAttributesList().forEach(attributeDto -> population.addAttribute(attributeDtos.fromDto(attributeDto)));
    }
    return population;
  }

  @NotNull
  private PopulationDto.SelectionCriteriaDto asDto(@NotNull Population.SelectionCriteria selectionCriteria) {
    PopulationDto.SelectionCriteriaDto.Builder builder = PopulationDto.SelectionCriteriaDto.newBuilder();
    if(selectionCriteria.getGender() != null) {
      builder.setGender(PopulationDto.SelectionCriteriaDto.Gender.valueOf(selectionCriteria.getGender().ordinal()));
    }
    if(selectionCriteria.getAgeMin() != null) {
      builder.setAgeMin(selectionCriteria.getAgeMin());
    }
    if(selectionCriteria.getAgeMax() != null) {
      builder.setAgeMax(selectionCriteria.getAgeMax());
    }
    if(selectionCriteria.getCountriesIso() != null) {
      selectionCriteria.getCountriesIso().forEach(builder::addCountriesIso);
    }
    if(selectionCriteria.getTerritory() != null) {
      builder.addAllTerritory(localizedStringDtos.asDto(selectionCriteria.getTerritory()));
    }
    if(selectionCriteria.getCriteria() != null) {
      selectionCriteria.getCriteria().forEach(builder::addCriteria);
    }
    if(selectionCriteria.getEthnicOrigin() != null) {
      builder.addAllEthnicOrigin(localizedStringDtos.asDtoList(selectionCriteria.getEthnicOrigin()));
    }
    if(selectionCriteria.getHealthStatus() != null) {
      builder.addAllHealthStatus(localizedStringDtos.asDtoList(selectionCriteria.getHealthStatus()));
    }
    if(selectionCriteria.getOtherCriteria() != null) {
      builder.addAllOtherCriteria(localizedStringDtos.asDto(selectionCriteria.getOtherCriteria()));
    }
    if(selectionCriteria.getInfo() != null) builder.addAllInfo(localizedStringDtos.asDto(selectionCriteria.getInfo()));
    return builder.build();
  }

  @NotNull
  private Population.SelectionCriteria fromDto(PopulationDto.SelectionCriteriaDto dto) {
    Population.SelectionCriteria selectionCriteria = new Population.SelectionCriteria();
    if(dto.hasGender()) {
      selectionCriteria.setGender(Population.SelectionCriteria.Gender.valueOf(dto.getGender().name()));
    }
    if(dto.hasAgeMin()) selectionCriteria.setAgeMin(dto.getAgeMin());
    if(dto.hasAgeMax()) selectionCriteria.setAgeMax(dto.getAgeMax());
    if(dto.getCountriesIsoCount() > 0) selectionCriteria.setCountriesIso(dto.getCountriesIsoList());
    if(dto.getTerritoryCount() > 0) selectionCriteria.setTerritory(localizedStringDtos.fromDto(dto.getTerritoryList()));
    if(dto.getCriteriaCount() > 0) selectionCriteria.setCriteria(dto.getCriteriaList());
    if(dto.getEthnicOriginCount() > 0) {
      selectionCriteria.setEthnicOrigin(localizedStringDtos.fromDtoList(dto.getEthnicOriginList()));
    }
    if(dto.getHealthStatusCount() > 0) {
      selectionCriteria.setHealthStatus(localizedStringDtos.fromDtoList(dto.getHealthStatusList()));
    }
    if(dto.getOtherCriteriaCount() > 0) {
      selectionCriteria.setOtherCriteria(localizedStringDtos.fromDto(dto.getOtherCriteriaList()));
    }
    if(dto.getInfoCount() > 0) selectionCriteria.setInfo(localizedStringDtos.fromDto(dto.getInfoList()));
    return selectionCriteria;
  }

  @NotNull
  private PopulationDto.RecruitmentDto asDto(@NotNull Population.Recruitment recruitment) {
    PopulationDto.RecruitmentDto.Builder builder = PopulationDto.RecruitmentDto.newBuilder();
    if(recruitment.getDataSources() != null) {
      recruitment.getDataSources().forEach(builder::addDataSources);
    }
    if(recruitment.getGeneralPopulationSources() != null) {
      recruitment.getGeneralPopulationSources().forEach(builder::addGeneralPopulationSources);
    }
    if(recruitment.getSpecificPopulationSources() != null) {
      recruitment.getSpecificPopulationSources().forEach(builder::addSpecificPopulationSources);
    }
    if(recruitment.getOtherSpecificPopulationSource() != null) {
      builder.addAllOtherSpecificPopulationSource(
          localizedStringDtos.asDto(recruitment.getOtherSpecificPopulationSource()));
    }
    if(recruitment.getStudies() != null) {
      builder.addAllStudies(localizedStringDtos.asDtoList(recruitment.getStudies()));
    }
    if(recruitment.getOtherSource() != null) {
      builder.addAllOtherSource(localizedStringDtos.asDto(recruitment.getOtherSource()));
    }
    if(recruitment.getInfo() != null) builder.addAllInfo(localizedStringDtos.asDto(recruitment.getInfo()));
    return builder.build();
  }

  @NotNull
  private Population.Recruitment fromDto(@NotNull PopulationDto.RecruitmentDtoOrBuilder dto) {
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
          .setOtherSpecificPopulationSource(localizedStringDtos.fromDto(dto.getOtherSpecificPopulationSourceList()));
    }
    if(dto.getStudiesCount() > 0) {
      recruitment.setStudies(localizedStringDtos.fromDtoList(dto.getStudiesList()));
    }
    if(dto.getOtherSourceCount() > 0) {
      recruitment.setOtherSource(localizedStringDtos.fromDto(dto.getOtherSourceList()));
    }
    if(dto.getInfoCount() > 0) recruitment.setInfo(localizedStringDtos.fromDto(dto.getInfoList()));

    return recruitment;
  }

  @NotNull
  PopulationDto.DataCollectionEventDto asDto(@NotNull DataCollectionEvent dce) {
    PopulationDto.DataCollectionEventDto.Builder builder = PopulationDto.DataCollectionEventDto.newBuilder();
    builder.setId(dce.getId());
    if(dce.getName() != null) builder.addAllName(localizedStringDtos.asDto(dce.getName()));
    if(dce.getDescription() != null) builder.addAllDescription(localizedStringDtos.asDto(dce.getDescription()));
    if(dce.getStart() != null) {
      PersistableYearMonth.YearMonthData startData = dce.getStart().getYearMonthData();
      builder.setStartYear(startData.getYear()).setStartMonth(startData.getMonth());
    }
    if(dce.getEnd() != null) {
      PersistableYearMonth.YearMonthData endData = dce.getEnd().getYearMonthData();
      builder.setEndYear(endData.getYear()).setEndMonth(endData.getMonth());
    }
    if(dce.getDataSources() != null) dce.getDataSources().forEach(builder::addDataSources);
    if(dce.getAdministrativeDatabases() != null) {
      dce.getAdministrativeDatabases().forEach(builder::addAdministrativeDatabases);
    }
    if(dce.getOtherDataSources() != null) {
      builder.addAllOtherDataSources(localizedStringDtos.asDto(dce.getOtherDataSources()));
    }
    if(dce.getBioSamples() != null) dce.getBioSamples().forEach(builder::addBioSamples);
    if(dce.getTissueTypes() != null) builder.addAllTissueTypes(localizedStringDtos.asDto(dce.getTissueTypes()));
    if(dce.getOtherBioSamples() != null) {
      builder.addAllOtherBioSamples(localizedStringDtos.asDto(dce.getOtherBioSamples()));
    }
    if(dce.getAttributes() != null) {
      dce.getAttributes().asAttributeList().forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }
    return builder.build();
  }

  @NotNull
  DataCollectionEvent fromDto(@NotNull PopulationDto.DataCollectionEventDto dto) {
    DataCollectionEvent dce = new DataCollectionEvent();
    dce.setId(dto.getId());
    if(dto.getNameCount() > 0) dce.setName(localizedStringDtos.fromDto(dto.getNameList()));
    if(dto.getDescriptionCount() > 0) dce.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    if(dto.hasStartYear()) dce.setStart(dto.getStartYear(), dto.hasStartMonth() ? dto.getStartMonth() : null);
    if(dto.hasEndYear()) dce.setEnd(dto.getEndYear(), dto.hasEndMonth() ? dto.getEndMonth() : null);
    if(dto.getDataSourcesCount() > 0) dce.setDataSources(dto.getDataSourcesList());
    if(dto.getAdministrativeDatabasesCount() > 0) dce.setAdministrativeDatabases(dto.getAdministrativeDatabasesList());
    if(dto.getOtherDataSourcesCount() > 0) {
      dce.setOtherDataSources(localizedStringDtos.fromDto(dto.getOtherDataSourcesList()));
    }
    if(dto.getBioSamplesCount() > 0) dce.setBioSamples(dto.getBioSamplesList());
    if(dto.getTissueTypesCount() > 0) dce.setTissueTypes(localizedStringDtos.fromDto(dto.getTissueTypesList()));
    if(dto.getOtherBioSamplesCount() > 0) {
      dce.setOtherBioSamples(localizedStringDtos.fromDto(dto.getOtherBioSamplesList()));
    }
    if(dto.getAttributesCount() > 0) {
      dto.getAttributesList().forEach(attributeDto -> dce.addAttribute(attributeDtos.fromDto(attributeDto)));
    }
    return dce;
  }
}
