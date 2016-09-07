package org.obiba.mica.web.model;

import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.obiba.mica.JSONUtils;
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
  private AttributeDtos attributeDtos;

  @NotNull
  PopulationDto asDto(Population population) {
    PopulationDto.Builder builder = PopulationDto.newBuilder();

    if(population.hasModel()) builder.setContent(JSONUtils.toJSON(population.getModel()));

    builder.setId(population.getId());

    if(population.getName() != null) builder.addAllName(localizedStringDtos.asDto(population.getName()));

    if(population.getDescription() != null) {
      builder.addAllDescription(localizedStringDtos.asDto(population.getDescription()));
    }

    if(population.getDataCollectionEvents() != null) {
      population.getDataCollectionEvents().forEach(dce -> builder.addDataCollectionEvents(asDto(dce)));
    }

    return builder.build();
  }

  @NotNull
  Population fromDto(Mica.StudyDto.PopulationDtoOrBuilder dto) {
    Population population = new Population();
    population.setId(dto.getId());
    if(dto.getNameCount() > 0) population.setName(localizedStringDtos.fromDto(dto.getNameList()));
    if(dto.getDescriptionCount() > 0) population.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    if(dto.getDataCollectionEventsCount() > 0) {
      dto.getDataCollectionEventsList().forEach(dceDto -> population.addDataCollectionEvent(fromDto(dceDto)));
    }

    if(dto.hasContent() && !Strings.isNullOrEmpty(dto.getContent())) population.setModel(JSONUtils.toMap(dto.getContent()));
    else {
      Map<String, Object> model = Maps.newHashMap();

      if (dto.hasRecruitment()) model.put("recruitment", fromDto(dto.getRecruitment()));
      if (dto.hasSelectionCriteria()) model.put("selectionCriteria", fromDto(dto.getSelectionCriteria()));
      if (dto.hasNumberOfParticipants()) {
        model.put("numberOfParticipants", numberOfParticipantsDtos.fromDto(dto.getNumberOfParticipants()));
      }

      population.setModel(model);
    }

    return population;
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

    if(dce.hasModel()) builder.setContent(JSONUtils.toJSON(dce.getModel()));

    builder.setId(dce.getId());

    if(dce.getName() != null) builder.addAllName(localizedStringDtos.asDto(dce.getName()));

    if(dce.getDescription() != null) builder.addAllDescription(localizedStringDtos.asDto(dce.getDescription()));

    if(dce.getStart() != null) {
      PersistableYearMonth.YearMonthData startData = dce.getStart().getYearMonthData();
      builder.setStartYear(startData.getYear());
      if (startData.getMonth() != 0) {
        builder.setStartMonth(startData.getMonth());
      }
    }

    if(dce.getEnd() != null) {
      PersistableYearMonth.YearMonthData endData = dce.getEnd().getYearMonthData();
      builder.setEndYear(endData.getYear());
      if (endData.getMonth() != 0) {
        builder.setEndMonth(endData.getMonth());
      }
    }

    return builder.build();
  }

  @NotNull
  DataCollectionEvent fromDto(@NotNull PopulationDto.DataCollectionEventDto dto) {
    DataCollectionEvent dce = new DataCollectionEvent();
    dce.setId(dto.getId());
    if(dto.getNameCount() > 0) dce.setName(localizedStringDtos.fromDto(dto.getNameList()));
    if(dto.hasStartYear()) dce.setStart(dto.getStartYear(), dto.hasStartMonth() ? dto.getStartMonth() : null);
    if(dto.hasEndYear()) dce.setEnd(dto.getEndYear(), dto.hasEndMonth() ? dto.getEndMonth() : null);

    if(dto.hasContent() && !Strings.isNullOrEmpty(dto.getContent())) dce.setModel(JSONUtils.toMap(dto.getContent()));
    else {
      Map<String, Object> model = Maps.newHashMap();

      if (dto.getDescriptionCount() > 0) model.put("description", localizedStringDtos.fromDto(dto.getDescriptionList()));
      if (dto.getDataSourcesCount() > 0) model.put("dataSources", dto.getDataSourcesList());
      if (dto.getAdministrativeDatabasesCount() > 0)
        model.put("administrativeDatabases", dto.getAdministrativeDatabasesList());
      if (dto.getOtherDataSourcesCount() > 0) {
        model.put("otherDataSources", localizedStringDtos.fromDto(dto.getOtherDataSourcesList()));
      }
      if (dto.getBioSamplesCount() > 0) model.put("bioSamples", dto.getBioSamplesList());
      if (dto.getTissueTypesCount() > 0) model.put("tissueTypes", localizedStringDtos.fromDto(dto.getTissueTypesList()));
      if (dto.getOtherBioSamplesCount() > 0) {
        model.put("otherBioSamples", localizedStringDtos.fromDto(dto.getOtherBioSamplesList()));
      }

      dce.setModel(model);
    }

    return dce;
  }
}
