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

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetCategory;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.domain.StudyTable;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.study.domain.StudyState;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
class DatasetDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttributeDtos attributeDtos;

  @Inject
  private StudySummaryDtos studySummaryDtos;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private StudyService studyService;

  @NotNull
  Mica.DatasetDto asDto(@NotNull StudyDataset dataset) {
    Mica.DatasetDto.Builder builder = asDtoBuilder(dataset);

    if(dataset.hasStudyTable()) {
      Mica.StudyDatasetDto.Builder sbuilder = Mica.StudyDatasetDto.newBuilder().setStudyTable(
          asDto(dataset.getStudyTable()).setStudySummary(getStudySummary(dataset.getStudyTable().getStudyId())));
      builder.setExtension(Mica.StudyDatasetDto.type, sbuilder.build());
    }
    return builder.build();
  }

  @NotNull
  Mica.DatasetDto asDto(@NotNull HarmonizationDataset dataset) {
    Mica.DatasetDto.Builder builder = asDtoBuilder(dataset);

    Mica.HarmonizationDatasetDto.Builder hbuilder = Mica.HarmonizationDatasetDto.newBuilder();
    hbuilder.setProject(dataset.getProject());
    hbuilder.setTable(dataset.getTable());
    if(!dataset.getStudyTables().isEmpty()) {
      dataset.getStudyTables().forEach(studyTable -> hbuilder.addStudyTables(asDto(studyTable)
          .setStudySummary(getStudySummary(studyTable.getStudyId()))));
    }
    builder.setExtension(Mica.HarmonizationDatasetDto.type, hbuilder.build());

    return builder.build();
  }

  @NotNull
  Mica.DatasetVariableResolverDto.Builder asDto(@NotNull DatasetVariable.IdResolver resolver) {
    Mica.DatasetVariableResolverDto.Builder builder = Mica.DatasetVariableResolverDto.newBuilder();

    builder.setId(resolver.getId()) //
        .setDatasetId(resolver.getDatasetId()) //
        .setName(resolver.getName()) //
        .setVariableType(resolver.getType().name());

    if(resolver.hasStudyId()) {
      builder.setStudyId(resolver.getStudyId());
    }

    return builder;
  }

  @NotNull
  Mica.DatasetVariableDto asDto(@NotNull DatasetVariable variable) {
    Mica.DatasetVariableDto.Builder builder = Mica.DatasetVariableDto.newBuilder() //
        .setId(variable.getId()) //
        .setDatasetId(variable.getDatasetId()) //
        .addAllDatasetName(localizedStringDtos.asDto(variable.getDatasetName())) //
        .setName(variable.getName()) //
        .setEntityType(variable.getEntityType()) //
        .setValueType(variable.getValueType())//
        .setVariableType(variable.getVariableType().name()) //
        .setRepeatable(variable.isRepeatable());

    if(variable.getStudyIds() != null) {
      builder.addAllStudyIds(variable.getStudyIds());
      for(String studyId : variable.getStudyIds()) {
        builder.addStudySummaries(getStudySummary(studyId));
      }
    }

    if(!Strings.isNullOrEmpty(variable.getOccurrenceGroup())) {
      builder.setOccurrenceGroup(variable.getOccurrenceGroup());
    }

    if(!Strings.isNullOrEmpty(variable.getUnit())) {
      builder.setUnit(variable.getUnit());
    }

    if(!Strings.isNullOrEmpty(variable.getReferencedEntityType())) {
      builder.setReferencedEntityType(variable.getReferencedEntityType());
    }

    if(!Strings.isNullOrEmpty(variable.getMimeType())) {
      builder.setMimeType(variable.getMimeType());
    }

    if(variable.getAttributes() != null) {
      variable.getAttributes().asAttributeList()
          .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }

    if(variable.getCategories() != null) {
      variable.getCategories().forEach(category -> builder.addCategories(asDto(category)));
    }

    return builder.build();
  }

  private Mica.DatasetCategoryDto asDto(DatasetCategory category) {
    Mica.DatasetCategoryDto.Builder builder = Mica.DatasetCategoryDto.newBuilder() //
        .setName(category.getName()) //
        .setMissing(category.isMissing());

    if(category.getAttributes() != null) {
      category.getAttributes().asAttributeList()
          .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }

    return builder.build();
  }

  private Mica.DatasetDto.StudyTableDto.Builder asDto(StudyTable studyTable) {
    Mica.DatasetDto.StudyTableDto.Builder builder = Mica.DatasetDto.StudyTableDto.newBuilder() //
        .setStudyId(studyTable.getStudyId()) //
        .setProject(studyTable.getProject()) //
        .setTable(studyTable.getTable());

    return builder;
  }

  private Mica.DatasetDto.Builder asDtoBuilder(Dataset dataset) {
    Mica.DatasetDto.Builder builder = Mica.DatasetDto.newBuilder();
    if(dataset.getId() != null) builder.setId(dataset.getId());
    builder.setEntityType(dataset.getEntityType());
    if(dataset.getName() != null) builder.addAllName(localizedStringDtos.asDto(dataset.getName()));
    if(dataset.getDescription() != null) {
      builder.addAllDescription(localizedStringDtos.asDto(dataset.getDescription()));
    }
    builder.setPublished(dataset.isPublished());
    if(dataset.getAttributes() != null) {
      dataset.getAttributes().asAttributeList()
          .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }
    return builder;
  }

  @NotNull
  public Dataset fromDto(Mica.DatasetDto dto) {
    Dataset dataset;
    if(dto.hasExtension(Mica.HarmonizationDatasetDto.type)) {
      HarmonizationDataset harmonizationDataset = new HarmonizationDataset();
      Mica.HarmonizationDatasetDto ext = dto.getExtension(Mica.HarmonizationDatasetDto.type);
      harmonizationDataset.setProject(ext.getProject());
      harmonizationDataset.setTable(ext.getTable());
      if(ext.getStudyTablesCount() > 0) {
        ext.getStudyTablesList().forEach(tableDto -> harmonizationDataset.addStudyTable(fromDto(tableDto)));
      }
      dataset = harmonizationDataset;
    } else {
      StudyDataset studyDataset = new StudyDataset();
      Mica.StudyDatasetDto ext = dto.getExtension(Mica.StudyDatasetDto.type);
      studyDataset.setStudyTable(fromDto(ext.getStudyTable()));
      dataset = studyDataset;
    }
    if(dto.hasId()) dataset.setId(dto.getId());
    dataset.setName(localizedStringDtos.fromDto(dto.getNameList()));
    dataset.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    dataset.setEntityType(dto.getEntityType());
    dataset.setPublished(dto.getPublished());
    if(dto.getAttributesCount() > 0) {
      dto.getAttributesList().forEach(attributeDto -> dataset.addAttribute(attributeDtos.fromDto(attributeDto)));
    }
    return dataset;
  }

  private StudyTable fromDto(Mica.DatasetDto.StudyTableDto dto) {
    StudyTable table = new StudyTable();
    table.setStudyId(dto.getStudyId());
    table.setProject(dto.getProject());
    table.setTable(dto.getTable());
    return table;
  }

  private Mica.StudySummaryDto getStudySummary(String studyId) {
    StudyState studyState = studyService.findStateById(studyId);
    if (studyState.isPublished()) {
      return studySummaryDtos.asDto(publishedStudyService.findById(studyId));
    }

    return studySummaryDtos.asDto(studyState);
  }
}
