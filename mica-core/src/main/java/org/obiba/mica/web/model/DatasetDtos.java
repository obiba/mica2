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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.core.domain.*;
import org.obiba.mica.core.source.OpalTableSource;
import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.*;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
class DatasetDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttributeDtos attributeDtos;

  @Inject
  private EntityStateDtos entityStateDtos;

  @Inject
  private StudySummaryDtos studySummaryDtos;

  @Inject
  private PermissionsDtos permissionsDtos;

  @Inject
  private TaxonomyDtos taxonomyDtos;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private StudyDatasetStateRepository studyDatasetStateRepository;

  @Inject
  private HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private SubjectAclService subjectAclService;

  @NotNull
  Mica.DatasetDto.Builder asDtoBuilder(@NotNull StudyDataset dataset, boolean asDraft, boolean studySummary) {
    Mica.DatasetDto.Builder builder = asBuilder(dataset);
    builder.setVariableType(DatasetVariable.Type.Collected.name());

    if(dataset.hasStudyTable() && !Strings.isNullOrEmpty(dataset.getStudyTable().getStudyId()) &&
      isStudyTablePermitted(asDraft, "individual", dataset.getStudyTable().getStudyId())) {
      builder.setCollected(Mica.CollectedDatasetDto.newBuilder().setStudyTable(asDto(dataset.getStudyTable(), studySummary)));
    }

    Mica.PermissionsDto permissionsDto = permissionsDtos.asDto(dataset);

    if(asDraft) {
      Optional<StudyDatasetState> state = studyDatasetStateRepository.findById(dataset.getId());
      if(state.isPresent()) {
        builder.setPublished(state.get().isPublished());
        Mica.EntityStateDto.Builder stateBuilder = entityStateDtos.asDto(state.get()).setPermissions(permissionsDto);
        builder.setState(stateBuilder.setPermissions(permissionsDto).setRequireIndexing(state.get().isRequireIndexing()).build());
      }
    }

    builder.setPermissions(permissionsDto);
    return builder;
  }

  @NotNull
  Mica.DatasetDto asDto(@NotNull StudyDataset dataset) {
    return asDto(dataset, false, true);
  }

  @NotNull
  Mica.DatasetDto asDto(@NotNull StudyDataset dataset, boolean asDraft, boolean studySummary) {
    return asDtoBuilder(dataset, asDraft, studySummary).build();
  }

  @NotNull
  Mica.DatasetDto.Builder asDtoBuilder(@NotNull HarmonizationDataset dataset, boolean asDraft, boolean studySummary) {
    Mica.DatasetDto.Builder builder = asBuilder(dataset);
    builder.setVariableType(DatasetVariable.Type.Dataschema.name());
    Mica.HarmonizedDatasetDto.Builder hBuilder = Mica.HarmonizedDatasetDto.newBuilder();

    if(dataset.hasHarmonizationTable() && !Strings.isNullOrEmpty(dataset.getHarmonizationTable().getStudyId()) &&
      isStudyTablePermitted(asDraft, "harmonization", dataset.getHarmonizationTable().getStudyId())) {
      hBuilder.setHarmonizationTable(
        createHarmonizationLinkDtoFromHarmonizationTable(dataset.getHarmonizationTable(), asDraft));
    }

    if(!dataset.getStudyTables().isEmpty()) {
      dataset.getStudyTables().stream()
        .filter(studyTable -> isStudyTablePermitted(asDraft, "individual", studyTable.getStudyId()))
        .forEach(studyTable -> hBuilder.addStudyTables(asDto(studyTable, studySummary)));
    }

    if(!dataset.getHarmonizationTables().isEmpty()) {
      dataset.getHarmonizationTables().stream()
        .filter(studyTable -> isStudyTablePermitted(asDraft, "harmonization", studyTable.getStudyId()))
        .forEach(harmonizationTable -> hBuilder.addHarmonizationTables(asDto(harmonizationTable, studySummary)));
    }

    builder.setProtocol(hBuilder);

    Mica.PermissionsDto permissionsDto = permissionsDtos.asDto(dataset);
    if(asDraft) {
      Optional<HarmonizationDatasetState> state = harmonizationDatasetStateRepository.findById(dataset.getId());
      if(state.isPresent()) {
        builder.setPublished(state.get().isPublished());
        builder.setState(entityStateDtos.asDto(state.get()).setPermissions(permissionsDto).build());
      }
    }

    builder.setPermissions(permissionsDto);

    return builder;
  }

  public boolean isStudyTablePermitted(boolean asDraft, String type, String id) {
    return (asDraft && subjectAclService.isPermitted(String.format("/draft/%s-study", type), "VIEW", id)) ||
      subjectAclService.isAccessible(String.format("/%s-study", type), id);
  }

  @NotNull
  Mica.DatasetDto asDto(@NotNull HarmonizationDataset dataset) {
    return asDto(dataset, false, true);
  }

  @NotNull
  Mica.DatasetDto asDto(@NotNull HarmonizationDataset dataset, boolean asDraft, boolean studySummary) {
    return asDtoBuilder(dataset, asDraft, studySummary).build();
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
    if(resolver.hasSource()) {
      builder.setSource(resolver.getSource());
      if (OpalTableSource.isFor(resolver.getSource())) {
        OpalTableSource source = OpalTableSource.fromURN(resolver.getSource());
        builder.setProject(source.getProject());
        builder.setTable(source.getTable());
      }
    }

    return builder;
  }


  @NotNull
  Mica.DatasetVariableResolverDto.Builder asDto(@NotNull DatasetVariable.IdResolver resolver, DatasetVariable variable) {
    Mica.DatasetVariableResolverDto.Builder builder = asDto(resolver);

    builder.addAllDatasetAcronym(localizedStringDtos.asDto(variable.getDatasetAcronym()));
    builder.addAllDatasetName(localizedStringDtos.asDto(variable.getDatasetName()));

    if (variable.hasAttributes()) {
      if (variable.hasAttribute("label", null)) {
        builder.addAllVariableLabel(localizedStringDtos.asDto(variable.getAttributes().getAttribute("label", null).getValues()));
      }
      if (variable.hasAttribute("description", null)) {
        builder.addAllDescription(localizedStringDtos.asDto(variable.getAttributes().getAttribute("description", null).getValues()));
      }
      variable.getAttributes().asAttributeList().stream()
        .filter(Attribute::hasNamespace)
        .filter(attr -> !Strings.isNullOrEmpty(attr.getValues().getUndetermined()))
        .forEach(attr -> builder.addAnnotations(Mica.AnnotationDto.newBuilder()
          .setTaxonomy(attr.getNamespace())
          .setVocabulary(attr.getName())
          .setValue(attr.getValues().getUndetermined())));
    }

    if (!Strings.isNullOrEmpty(variable.getPopulationId())) {
      builder.setPopulationId(variable.getPopulationId());
    }

    if (!Strings.isNullOrEmpty(variable.getDceId())) {
      builder.setDceId(variable.getDceId());
    }

    if (!Strings.isNullOrEmpty(variable.getValueType())) {
      builder.setValueType(variable.getValueType());
    }

    if (!Strings.isNullOrEmpty(variable.getEntityType())) {
      builder.setEntityType(variable.getEntityType());
    }

    if (!Strings.isNullOrEmpty(variable.getReferencedEntityType())) {
      builder.setReferencedEntityType(variable.getReferencedEntityType());
    }

    if (!Strings.isNullOrEmpty(variable.getMimeType())) {
      builder.setMimeType(variable.getMimeType());
    }

    if (variable.hasRepeatable()) {
      builder.setRepeatable(variable.isRepeatable());
    }

    if (!Strings.isNullOrEmpty(variable.getOccurrenceGroup())) {
      builder.setOccurrenceGroup(variable.getOccurrenceGroup());
    }

    if (!Strings.isNullOrEmpty(variable.getNature())) {
      builder.setNature(variable.getNature());
    }

    if (!Strings.isNullOrEmpty(variable.getUnit())) {
      builder.setUnit(variable.getUnit());
    }

    if(variable.getCategories() != null) {
      variable.getCategories().forEach(category -> builder.addCategories(asDto(category)));
    }

    return builder;
  }

  @NotNull
  Mica.DatasetVariableDto asDto(@NotNull DatasetVariable variable) {
    return asDto(variable, Collections.emptyList(), "en");
  }

  List<Mica.DatasetVariableDto> asDtos(Map<String, List<DatasetVariable>> studyIdVariableMap,
    @NotNull List<Taxonomy> taxonomies, String locale) {
    List<Mica.DatasetVariableDto> variableDtos = Lists.newArrayList();
    List<Mica.StudySummaryDto> studySummaryDtos = this.studySummaryDtos.asDtos(studyIdVariableMap.keySet());

    for(Mica.StudySummaryDto dto : studySummaryDtos) {
      variableDtos.addAll(studyIdVariableMap.get(dto.getId()).stream().map(variable -> {
        Mica.DatasetVariableDto.Builder builder = asDtoBuilder(variable, taxonomies, locale);

        if(variable.getStudyId() != null) {
          builder.setStudyId(variable.getStudyId());
          builder.setStudySummary(dto);
        }

        return builder.build();
      }).collect(Collectors.toList()));
    }

    return variableDtos;
  }

  @NotNull
  Mica.DatasetVariableDto asDto(@NotNull DatasetVariable variable, @NotNull List<Taxonomy> taxonomies, String locale) {
    Mica.DatasetVariableDto.Builder builder = asDtoBuilder(variable, taxonomies, locale);

    if(variable.getStudyId() != null) {
      builder.setStudyId(variable.getStudyId());
      builder.setStudySummary(studySummaryDtos.asDtoFromDceUid(variable.getDceId()));
    }

    return builder.build();
  }

  @NotNull
  Mica.DatasetVariableSummaryDto asSummaryDto(@NotNull DatasetVariable variable, BaseStudyTable studyTable) {
    return asSummaryDto(variable, studyTable, false);
  }

  @NotNull
  Mica.DatasetHarmonizedVariableSummaryDto asHarmonizedSummaryDto(@NotNull DatasetVariable variable) {
    if (variable == null) return Mica.DatasetHarmonizedVariableSummaryDto.newBuilder().setStatus("").build();
    Mica.DatasetHarmonizedVariableSummaryDto.Builder builder = Mica.DatasetHarmonizedVariableSummaryDto.newBuilder() //
      .setHarmonizedVariableRef(asDto(DatasetVariable.IdResolver.from(variable.getId())));

    if(variable.getAttributes() != null) {
      variable.getAttributes().asAttributeList()
        .forEach(attribute -> {
          if ("Mlstr_harmo".equals(attribute.getNamespace()) && "status".equals(attribute.getName()))
            builder.setStatus(attribute.getValues().getUndetermined());

          if ("Mlstr_harmo".equals(attribute.getNamespace()) && "status_detail".equals(attribute.getName()))
            builder.setStatusDetail(attribute.getValues().getUndetermined());
        });
    }

    return builder.build();
  }

  @NotNull
  Mica.DatasetVariableSummaryDto asSummaryDto(@NotNull DatasetVariable variable, BaseStudyTable studyTable,
    boolean includeSummaries) {
    Mica.DatasetVariableSummaryDto.Builder builder = Mica.DatasetVariableSummaryDto.newBuilder() //
      .setResolver(asDto(DatasetVariable.IdResolver.from(variable.getId())));

    if(variable.getAttributes() != null) {
      variable.getAttributes().asAttributeList()
        .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }

    if(studyTable instanceof StudyTable) builder.setStudyTable(asDto((StudyTable) studyTable, includeSummaries));
    else if(studyTable instanceof HarmonizationStudyTable) {
      builder.setHarmonizationStudyTable(asDto((HarmonizationStudyTable) studyTable, includeSummaries));
    }

    return builder.build();
  }

  private Mica.TermAttributesDto asDto(Taxonomy taxonomy, Attributes attributes, String locale) {
    Mica.TermAttributesDto.Builder builder = Mica.TermAttributesDto.newBuilder() //
      .setTaxonomy(taxonomyDtos.asDto(taxonomy, locale));

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
            termBuilder.setVocabulary(taxonomyDtos.asDto(vocabulary, locale));
          }

          Term term = vocabulary.getTerm(termStr);
          termBuilder.addTerms(taxonomyDtos.asDto(term, locale));
        }
      }
    });

    // keep vocabulary order
    taxonomy.getVocabularies().forEach(vocabulary -> {
      if(terms.containsKey(vocabulary.getName())) {
        builder.addVocabularyTerms(terms.get(vocabulary.getName()));
      }
    });

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

  public Mica.DatasetDto.StudyTableDto.Builder asDto(StudyTable studyTable) {
    return asDto(studyTable, false);
  }

  public Mica.DatasetDto.StudyTableDto.Builder asDto(StudyTable studyTable, boolean includeSummary) {
    Mica.DatasetDto.StudyTableDto.Builder sbuilder = Mica.DatasetDto.StudyTableDto.newBuilder()
      .setSource(studyTable.getSource())
      .setWeight(studyTable.getWeight())
      .setStudyId(studyTable.getStudyId())
      .setDceId(studyTable.getDataCollectionEventUId());

    if (OpalTableSource.isFor(studyTable.getSource())) {
      OpalTableSource source = OpalTableSource.fromURN(studyTable.getSource());
      sbuilder.setProject(source.getProject());
      sbuilder.setTable(source.getTable());
    }

    String populationId = studyTable.getPopulationId();
    if (!Strings.isNullOrEmpty(populationId)) {
      sbuilder.setPopulationId(populationId);
    }

    String dceId = studyTable.getDataCollectionEventId();
    if (!Strings.isNullOrEmpty(dceId)) {
      sbuilder.setDataCollectionEventId(dceId);
    }

    String tableUid = studyTable.getTableUniqueId();
    if (!Strings.isNullOrEmpty(tableUid)) {
      sbuilder.setTableUid(tableUid);
    }

    if(includeSummary) sbuilder.setStudySummary(studySummaryDtos.asDto(studyTable.getStudyId()));

    sbuilder.addAllName(localizedStringDtos.asDto(studyTable.getName()));
    sbuilder.addAllDescription(localizedStringDtos.asDto(studyTable.getDescription()));
    sbuilder.addAllAdditionalInformation(localizedStringDtos.asDto(studyTable.getAdditionalInformation()));

    return sbuilder;
  }

  public Mica.DatasetDto.HarmonizationTableDto.Builder asDto(HarmonizationStudyTable harmonizationTable) {
    return asDto(harmonizationTable, false);
  }

  public Mica.DatasetDto.HarmonizationTableDto.Builder asDto(HarmonizationStudyTable harmonizationTable,
    boolean includeSummary) {
    Mica.DatasetDto.HarmonizationTableDto.Builder hBuilder = Mica.DatasetDto.HarmonizationTableDto.newBuilder()
      .setSource(harmonizationTable.getSource())
      .setWeight(harmonizationTable.getWeight())
      .setStudyId(harmonizationTable.getStudyId());

    if (OpalTableSource.isFor(harmonizationTable.getSource())) {
      OpalTableSource source = OpalTableSource.fromURN(harmonizationTable.getSource());
      hBuilder.setProject(source.getProject());
      hBuilder.setTable(source.getTable());
    }

    String tableUid = harmonizationTable.getTableUniqueId();
    if (!Strings.isNullOrEmpty(tableUid)) {
      hBuilder.setTableUid(tableUid);
    }

    if(includeSummary) hBuilder.setStudySummary(studySummaryDtos.asDto(harmonizationTable.getStudyId()));

    hBuilder.addAllName(localizedStringDtos.asDto(harmonizationTable.getName()));
    hBuilder.addAllDescription(localizedStringDtos.asDto(harmonizationTable.getDescription()));
    hBuilder.addAllAdditionalInformation(localizedStringDtos.asDto(harmonizationTable.getAdditionalInformation()));

    return hBuilder;
  }

  public Mica.DatasetVariableAggregationDto.Builder asDto(@NotNull BaseStudyTable studyTable,
    @Nullable Mica.DatasetVariableAggregationDto summary, boolean withStudySummary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = summary == null ? Mica.DatasetVariableAggregationDto.newBuilder() : summary.toBuilder();

    if (summary == null) {
      aggDto.setTotal(0).setN(0);
    }

    if(studyTable instanceof StudyTable)
      aggDto.setStudyTable(asDto((StudyTable) studyTable, withStudySummary));
    else if (studyTable instanceof HarmonizationStudyTable)
      aggDto.setHarmonizationStudyTable(asDto((HarmonizationStudyTable) studyTable, withStudySummary));

    return aggDto;
  }

  public Mica.DatasetVariableContingencyDto.Builder asContingencyDto(@NotNull BaseStudyTable studyTable, @Nullable Mica.DatasetVariableContingencyDto crossDto) {
    Mica.DatasetVariableContingencyDto.Builder crossDtoBuilder = crossDto == null ? Mica.DatasetVariableContingencyDto.newBuilder() : crossDto.toBuilder();

    if(studyTable instanceof StudyTable)
      crossDtoBuilder.setStudyTable(asDto((StudyTable) studyTable, true));
    else if (studyTable instanceof HarmonizationStudyTable)
      crossDtoBuilder.setHarmonizationStudyTable(asDto((HarmonizationStudyTable) studyTable));

    if(crossDto == null) {
      Mica.DatasetVariableAggregationDto.Builder allAggBuilder = Mica.DatasetVariableAggregationDto.newBuilder();
      allAggBuilder.setN(0);
      allAggBuilder.setTotal(0);
      crossDtoBuilder.setAll(allAggBuilder);
      return crossDtoBuilder;
    }

    return crossDtoBuilder;
  }

  private Mica.DatasetDto.Builder asBuilder(Dataset dataset) {
    Mica.DatasetDto.Builder builder = Mica.DatasetDto.newBuilder();
    if(dataset.getId() != null) builder.setId(dataset.getId());
    builder.setTimestamps(TimestampsDtos.asDto(dataset)) //
      .setEntityType(dataset.getEntityType()) //
      .addAllName(localizedStringDtos.asDto(dataset.getName())) //
      .addAllAcronym(localizedStringDtos.asDto(dataset.getAcronym())) //
      .addAllDescription(localizedStringDtos.asDto(dataset.getDescription()));

    if(dataset.getAttributes() != null) {
      dataset.getAttributes().asAttributeList()
        .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }

    if(dataset.hasModel()) {
      builder.setContent(JSONUtils.toJSON(dataset.getModel()));
    }

    return builder;
  }

  @NotNull
  public Dataset fromDto(Mica.DatasetDto dto) {
    Dataset dataset = dto.hasProtocol()
      ? fromDto(dto.getProtocol())
      : dto.hasCollected()
        ? fromDto(dto.getCollected())
        : new StudyDataset();

    if(dto.hasId()) dataset.setId(dto.getId() );
    dataset.setAcronym(localizedStringDtos.fromDto(dto.getAcronymList()));
    dataset.setName(localizedStringDtos.fromDto(dto.getNameList()));
    dataset.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    dataset.setEntityType(dto.getEntityType());
    dataset.setPublished(dto.getPublished());
    if(dto.getAttributesCount() > 0) {
      dto.getAttributesList().forEach(attributeDto -> dataset.addAttribute(attributeDtos.fromDto(attributeDto)));
    }
    if(dto.hasContent()) {
      dataset.setModel(JSONUtils.toMap(dto.getContent()));
    }

    return dataset;
  }

  private Dataset fromDto(@NotNull Mica.HarmonizedDatasetDto dto) {
    Assert.notNull(dto, "HarmonizationDataset dt cannot be null.");
    HarmonizationDataset harmonizationDataset = new HarmonizationDataset();


    if(dto.getStudyTablesCount() > 0) {
      dto.getStudyTablesList().forEach(tableDto -> harmonizationDataset.addStudyTable(fromDto(tableDto)));
    }

    if(dto.getHarmonizationTablesCount() > 0) {
      dto.getHarmonizationTablesList()
        .forEach(tableDto -> harmonizationDataset.addHarmonizationTable(fromDto(tableDto)));
    }

    if(dto.hasHarmonizationTable()) {
      HarmonizationStudyTable harmonizationLink = new HarmonizationStudyTable();
      // legacy
      if (dto.getHarmonizationTable().hasProject() && dto.getHarmonizationTable().hasTable()) {
        harmonizationLink.setSource(makesource(dto.getHarmonizationTable().getProject(), dto.getHarmonizationTable().getTable()));
      } else {
        harmonizationLink.setSource(dto.getHarmonizationTable().getSource());
      }
      harmonizationLink.setStudyId(dto.getHarmonizationTable().getStudyId());
      harmonizationDataset.setHarmonizationTable(harmonizationLink);
    }
    return harmonizationDataset;
  }

  private Dataset fromDto(@NotNull Mica.CollectedDatasetDto dto) {
    Assert.notNull(dto, "StudyDataset dt cannot be null.");
    StudyDataset studyDataset = new StudyDataset();
    Optional.ofNullable(dto).ifPresent(ext -> studyDataset.setStudyTable(fromDto(ext.getStudyTable())));
    return studyDataset;
  }

  private StudyTable fromDto(Mica.DatasetDto.StudyTableDto dto) {
    StudyTable table = new StudyTable();
    table.setStudyId(dto.getStudyId());
    if (dto.hasPopulationId()) {
      table.setPopulationId(dto.getPopulationId());
    }
    if (dto.hasDataCollectionEventId()) {
      table.setDataCollectionEventId(dto.getDataCollectionEventId());
    }
    table.setWeight(dto.getWeight());
    table.setName(localizedStringDtos.fromDto(dto.getNameList()));
    table.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    table.setAdditionalInformation(localizedStringDtos.fromDto(dto.getAdditionalInformationList()));

    if (dto.hasProject() && dto.hasTable()) {
      table.setSource(makesource(dto.getProject(), dto.getTable()));
    } else {
      table.setSource(dto.getSource());
    }

    return table;
  }

  private HarmonizationStudyTable fromDto(Mica.DatasetDto.HarmonizationTableDto dto) {
    HarmonizationStudyTable table = new HarmonizationStudyTable();
    table.setStudyId(dto.getStudyId());
    table.setWeight(dto.getWeight());
    table.setName(localizedStringDtos.fromDto(dto.getNameList()));
    table.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    table.setAdditionalInformation(localizedStringDtos.fromDto(dto.getAdditionalInformationList()));

    // legacy
    if (dto.hasProject() && dto.hasTable()) {
      table.setSource(makesource(dto.getProject(), dto.getTable()));
    } else {
      table.setSource(dto.getSource());
    }

    return table;
  }

  // legacy
  private String makesource(String project, String table) {
    return OpalTableSource.newSource(project, table).getURN();
  }

  private Mica.DatasetDto.HarmonizationTableDto.Builder createHarmonizationLinkDtoFromHarmonizationTable(
    HarmonizationStudyTable harmonizationLink, boolean asDraft) {
    Mica.DatasetDto.HarmonizationTableDto.Builder harmonizationLinkBuilder = Mica.DatasetDto.HarmonizationTableDto
      .newBuilder();

    if(!Strings.isNullOrEmpty(harmonizationLink.getSource())) {
      harmonizationLinkBuilder.setSource(harmonizationLink.getSource());
      if (OpalTableSource.isFor(harmonizationLink.getSource())) {
        OpalTableSource source = OpalTableSource.fromURN(harmonizationLink.getSource());
        harmonizationLinkBuilder.setProject(source.getProject());
        harmonizationLinkBuilder.setTable(source.getTable());
      }
    }

    String studyId = harmonizationLink.getStudyId();

    if(asDraft && studyId != null) {
      harmonizationLinkBuilder.setStudyId(harmonizationLink.getStudyId());
    } else if(!asDraft && studyId != null && publishedStudyService.findById(studyId) != null) {
      harmonizationLinkBuilder.setStudyId(harmonizationLink.getStudyId());
    } else {
      harmonizationLinkBuilder.setStudyId("-");
    }

    if(studyId != null) harmonizationLinkBuilder.setStudySummary(studySummaryDtos.asHarmoStudyDto(studyId));

    return harmonizationLinkBuilder;
  }

  private Mica.DatasetVariableDto.Builder asDtoBuilder(@NotNull DatasetVariable variable,
    @NotNull List<Taxonomy> taxonomies, String locale) {
    Mica.DatasetVariableDto.Builder builder = Mica.DatasetVariableDto.newBuilder()
      .setId(variable.getId())
      .setDatasetId(variable.getDatasetId())
      .addAllDatasetName(localizedStringDtos.asDto(variable.getDatasetName()))
      .setName(variable.getName())
      .setEntityType(variable.getEntityType())
      .setValueType(variable.getValueType())
      .setVariableType(variable.getVariableType().name())
      .setRepeatable(variable.isRepeatable())
      .setNature(variable.getNature())
      .setIndex(variable.getIndex());

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
          Mica.TermAttributesDto dto = asDto(taxonomy, variable.getAttributes(), locale);
          if(dto.getVocabularyTermsCount() > 0) builder.addTermAttributes(dto);
        });
      }
    }

    if(variable.getCategories() != null) {
      variable.getCategories().forEach(category -> builder.addCategories(asDto(category)));
    }

    return builder;
  }
}
