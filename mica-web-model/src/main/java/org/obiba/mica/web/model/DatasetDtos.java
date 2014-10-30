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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetCategory;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

@Component
class DatasetDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttributeDtos attributeDtos;

  @Inject
  private StudySummaryDtos studySummaryDtos;

  @Inject
  private TaxonomyDtos taxonomyDtos;

  @NotNull
  Mica.DatasetDto.Builder asDtoBuilder(@NotNull StudyDataset dataset) {
    Mica.DatasetDto.Builder builder = asBuilder(dataset);

    if(dataset.hasStudyTable() && !Strings.isNullOrEmpty(dataset.getStudyTable().getStudyId())) {
      Mica.StudyDatasetDto.Builder sbuilder = Mica.StudyDatasetDto.newBuilder().setStudyTable(
          asDto(dataset.getStudyTable()).setStudySummary(studySummaryDtos.asDto(dataset.getStudyTable().getStudyId())));
      builder.setExtension(Mica.StudyDatasetDto.type, sbuilder.build());
    }
    return builder;
  }

  @NotNull
  Mica.DatasetDto asDto(@NotNull StudyDataset dataset) {
    return asDtoBuilder(dataset).build();
  }

  @NotNull
  Mica.DatasetDto.Builder asDtoBuilder(@NotNull HarmonizationDataset dataset) {
    Mica.DatasetDto.Builder builder = asBuilder(dataset);

    Mica.HarmonizationDatasetDto.Builder hbuilder = Mica.HarmonizationDatasetDto.newBuilder();
    hbuilder.setProject(dataset.getProject());
    hbuilder.setTable(dataset.getTable());
    if(!dataset.getStudyTables().isEmpty()) {
      dataset.getStudyTables().forEach(studyTable -> hbuilder
          .addStudyTables(asDto(studyTable).setStudySummary(studySummaryDtos.asDto(studyTable.getStudyId()))));
    }
    builder.setExtension(Mica.HarmonizationDatasetDto.type, hbuilder.build());

    return builder;
  }

  @NotNull
  Mica.DatasetDto asDto(@NotNull HarmonizationDataset dataset) {
    return asDtoBuilder(dataset).build();
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
    return asDto(variable, Collections.emptyList());
  }

  @NotNull
  Mica.DatasetVariableDto asDto(@NotNull DatasetVariable variable, @NotNull List<Taxonomy> taxonomies) {
    Mica.DatasetVariableDto.Builder builder = Mica.DatasetVariableDto.newBuilder() //
        .setId(variable.getId()) //
        .setDatasetId(variable.getDatasetId()) //
        .addAllDatasetName(localizedStringDtos.asDto(variable.getDatasetName())) //
        .setName(variable.getName()) //
        .setEntityType(variable.getEntityType()) //
        .setValueType(variable.getValueType())//
        .setVariableType(variable.getVariableType().name()) //
        .setRepeatable(variable.isRepeatable()) //
        .setNature(variable.getNature()) //
        .setIndex(variable.getIndex());

    if(variable.getStudyIds() != null) {
      builder.addAllStudyIds(variable.getStudyIds());
      for(String studyId : variable.getStudyIds()) {
        builder.addStudySummaries(studySummaryDtos.asDto(studyId));
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
      if(taxonomies != null) {
        taxonomies.forEach(taxonomy -> {
          Mica.TermAttributesDto dto = asDto(taxonomy, variable.getAttributes());
          if(dto.getVocabularyTermsCount() > 0) builder.addTermAttributes(dto);
        });
      }
    }

    if(variable.getCategories() != null) {
      variable.getCategories().forEach(category -> builder.addCategories(asDto(category)));
    }

    return builder.build();
  }

  @NotNull
  Mica.DatasetVariableSummaryDto asSummaryDto(@NotNull DatasetVariable variable) {
    Mica.DatasetVariableSummaryDto.Builder builder = Mica.DatasetVariableSummaryDto.newBuilder() //
        .setResolver(asDto(DatasetVariable.IdResolver.from(variable.getId())));

    if(variable.getAttributes() != null) {
      variable.getAttributes().asAttributeList()
          .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }

    return builder.build();
  }

  private Mica.TermAttributesDto asDto(Taxonomy taxonomy, Attributes attributes) {
    Mica.TermAttributesDto.Builder builder = Mica.TermAttributesDto.newBuilder() //
        .setTaxonomy(taxonomyDtos.asDto(taxonomy));

    Map<String, Mica.TermAttributeDto.Builder> terms = Maps.newHashMap();
    attributes.getAttributes(taxonomy.getName()).forEach(attr -> {
      if(taxonomy.hasVocabulary(attr.getName())) {

        Vocabulary vocabulary = taxonomy.getVocabulary(attr.getName());
        String termStr = attr.getValues().getUndetermined();
        if(!Strings.isNullOrEmpty(termStr) && vocabulary.hasTerm(termStr)) {
          Mica.TermAttributeDto.Builder termBuilder;
          if(terms.containsKey(vocabulary.getName())) {
            termBuilder = terms.get(vocabulary.getName());
          } else {
            termBuilder = Mica.TermAttributeDto.newBuilder();
            terms.put(vocabulary.getName(), termBuilder);
            termBuilder.setVocabulary(taxonomyDtos.asDto(vocabulary));
          }

          Term term = vocabulary.getTerm(termStr);
          termBuilder.addTerms(taxonomyDtos.asDto(term));
        }
      }
    });

    terms.values().forEach(builder::addVocabularyTerms);

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
        .setPopulationId(studyTable.getPopulationId()) //
        .setDataCollectionEventId(studyTable.getDataCollectionEventId()) //
        .setProject(studyTable.getProject()) //
        .setTable(studyTable.getTable()) //
        .setDceId(studyTable.getDataCollectionEventUId());

    return builder;
  }

  private Mica.DatasetDto.Builder asBuilder(Dataset dataset) {
    Mica.DatasetDto.Builder builder = Mica.DatasetDto.newBuilder();
    if(dataset.getId() != null) builder.setId(dataset.getId());
    builder.setEntityType(dataset.getEntityType());
    builder.addAllName(localizedStringDtos.asDto(dataset.getName()));
    builder.addAllAcronym(localizedStringDtos.asDto(dataset.getAcronym()));
    builder.addAllDescription(localizedStringDtos.asDto(dataset.getDescription()));

    builder.setPublished(dataset.isPublished());
    if(dataset.getAttributes() != null) {
      dataset.getAttributes().asAttributeList()
          .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }
    return builder;
  }

  @NotNull
  public Dataset fromDto(Mica.DatasetDtoOrBuilder dto) {
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
    dataset.setAcronym(localizedStringDtos.fromDto(dto.getAcronymList()));
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
    table.setPopulationId(dto.getPopulationId());
    table.setDataCollectionEventId(dto.getDataCollectionEventId());
    table.setProject(dto.getProject());
    table.setTable(dto.getTable());
    return table;
  }
}
