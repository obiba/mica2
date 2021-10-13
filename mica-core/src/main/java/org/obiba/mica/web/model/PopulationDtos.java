/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import com.google.common.base.Strings;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.study.date.PersistableYearMonth;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.springframework.stereotype.Component;

import static org.obiba.mica.web.model.Mica.PopulationDto;

@Component
@SuppressWarnings({ "OverlyLongMethod", "OverlyCoupledClass" })
class PopulationDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

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

    builder.setWeight(population.getWeight());

    return builder.build();
  }

  @NotNull
  Population fromDto(Mica.PopulationDtoOrBuilder dto) {
    Population population = new Population();
    population.setId(dto.getId());
    if(dto.getNameCount() > 0) population.setName(localizedStringDtos.fromDto(dto.getNameList()));
    if(dto.getDescriptionCount() > 0) population.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    if(dto.getDataCollectionEventsCount() > 0) {
      dto.getDataCollectionEventsList().forEach(dceDto -> population.addDataCollectionEvent(fromDto(dceDto)));
    }

    if (dto.hasContent() && !Strings.isNullOrEmpty(dto.getContent()))
      population.setModel(JSONUtils.toMap(dto.getContent()));
    else
      population.setModel(new HashMap<>());

    if (dto.hasWeight()) {
      population.setWeight(dto.getWeight());
    }

    return population;
  }

  @NotNull
  PopulationDto.DataCollectionEventDto asDto(@NotNull DataCollectionEvent dce) {
    PopulationDto.DataCollectionEventDto.Builder builder = PopulationDto.DataCollectionEventDto.newBuilder();

    if(dce.hasModel()) builder.setContent(JSONUtils.toJSON(dce.getModel()));

    builder.setId(dce.getId());

    if(dce.getName() != null) builder.addAllName(localizedStringDtos.asDto(dce.getName()));

    if(dce.getDescription() != null) {
      builder.addAllDescription(localizedStringDtos.asDto(dce.getDescription()));
    }

    if(dce.getStart() != null) {
      PersistableYearMonth.YearMonthData startData = dce.getStart().getYearMonthData();
      builder.setStartYear(startData.getYear());
      if (startData.getMonth() != 0) {
        builder.setStartMonth(startData.getMonth());
      }
      if (startData.getDay() != null) {
        builder.setStartDay(startData.getDay().format(DateTimeFormatter.ISO_DATE));
      }
    }

    if(dce.getEnd() != null) {
      PersistableYearMonth.YearMonthData endData = dce.getEnd().getYearMonthData();
      builder.setEndYear(endData.getYear());
      if (endData.getMonth() != 0) {
        builder.setEndMonth(endData.getMonth());
      }
      if (endData.getDay() != null) {
        builder.setEndDay(endData.getDay().format(DateTimeFormatter.ISO_DATE));
      }
    }

    builder.setWeight(dce.getWeight());

    return builder.build();
  }

  @NotNull
  DataCollectionEvent fromDto(@NotNull PopulationDto.DataCollectionEventDto dto) {
    DataCollectionEvent dce = new DataCollectionEvent();
    dce.setId(dto.getId());
    if(dto.getNameCount() > 0) dce.setName(localizedStringDtos.fromDto(dto.getNameList()));
    if(dto.getDescriptionCount() > 0) dce.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    if(dto.hasStartYear()) {
      dce.setStart(dto.getStartYear(),
        dto.hasStartMonth() ? dto.getStartMonth() : null,
        Strings.isNullOrEmpty(dto.getStartDay()) ? null : LocalDate.parse(dto.getStartDay(), DateTimeFormatter.ISO_DATE)
      );
    }

    if(dto.hasEndYear()) {
      dce.setEnd(dto.getEndYear(),
        dto.hasEndMonth() ? dto.getEndMonth() : null,
        Strings.isNullOrEmpty(dto.getEndDay()) ? null :  LocalDate.parse(dto.getEndDay(), DateTimeFormatter.ISO_DATE)
      );
    }

    if(dto.hasContent() && !Strings.isNullOrEmpty(dto.getContent()))
      dce.setModel(JSONUtils.toMap(dto.getContent()));
    else
      dce.setModel(new HashMap<>());

    if (dto.hasWeight()) {
      dce.setWeight(dto.getWeight());
    }

    return dce;
  }
}
