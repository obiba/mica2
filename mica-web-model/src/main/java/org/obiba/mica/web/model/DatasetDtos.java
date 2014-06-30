/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Dataset;
import org.obiba.mica.domain.HarmonizedDataset;
import org.obiba.mica.domain.StudyDataset;
import org.obiba.mica.domain.StudyTable;
import org.springframework.stereotype.Component;

@Component
class DatasetDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttributeDtos attributeDtos;

  @NotNull
  Mica.DatasetDto asDto(@NotNull StudyDataset dataset) {
    Mica.DatasetDto.Builder builder = asDtoBuilder(dataset);

    if(dataset.hasStudyTable()) {
      builder.setExtension(Mica.StudyDatasetDto.type,
          Mica.StudyDatasetDto.newBuilder().setStudyTable(asDto(dataset.getStudyTable())).build());
    }
    return builder.build();
  }

  @NotNull
  Mica.DatasetDto asDto(@NotNull HarmonizedDataset dataset) {
    Mica.DatasetDto.Builder builder = asDtoBuilder(dataset);

    Mica.HarmonizedDatasetDto.Builder hbuilder = Mica.HarmonizedDatasetDto.newBuilder();
    hbuilder.setTable(dataset.getTable());
    if(!dataset.getStudyTables().isEmpty()) {
      hbuilder.addAllStudyTables(dataset.getStudyTables().stream().map(this::asDto).collect(Collectors.toList()));
    }
    builder.setExtension(Mica.HarmonizedDatasetDto.type, hbuilder.build());

    return builder.build();
  }

  private Mica.DatasetDto.StudyTableDto asDto(StudyTable studyTable) {
    Mica.DatasetDto.StudyTableDto.Builder builder = Mica.DatasetDto.StudyTableDto.newBuilder() //
        .setStudyId(studyTable.getStudyId()) //
        .setProject(studyTable.getProject()) //
        .setTable(studyTable.getTable());

    return builder.build();
  }

  private Mica.DatasetDto.Builder asDtoBuilder(Dataset dataset) {
    Mica.DatasetDto.Builder builder = Mica.DatasetDto.newBuilder();
    builder.setId(dataset.getId());
    if(dataset.getName() != null) builder.addAllName(localizedStringDtos.asDto(dataset.getName()));
    if(dataset.getDescription() != null) {
      builder.addAllDescription(localizedStringDtos.asDto(dataset.getDescription()));
    }
    if(dataset.getAttributes() != null) {
      dataset.getAttributes().values().forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }
    return builder;
  }
}
