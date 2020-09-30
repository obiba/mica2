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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.magma.type.BooleanType;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.core.domain.*;
import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetCategory;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
      Mica.CollectedDatasetDto.Builder sbuilder = Mica.CollectedDatasetDto.newBuilder()
        .setStudyTable(asDto(dataset.getStudyTable(), studySummary));
      builder.setExtension(Mica.CollectedDatasetDto.type, sbuilder.build());
    }

    Mica.PermissionsDto permissionsDto = permissionsDtos.asDto(dataset);

    if(asDraft) {
      StudyDatasetState state = studyDatasetStateRepository.findOne(dataset.getId());
      if(state != null) {
        builder.setPublished(state.isPublished());
        Mica.EntityStateDto.Builder stateBuilder = entityStateDtos.asDto(state).setPermissions(permissionsDto);
        builder.setExtension(
          Mica.EntityStateDto.datasetState,
          stateBuilder.setPermissions(permissionsDto).setRequireIndexing(state.isRequireIndexing()).build()
        );
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

    Mica.HarmonizedDatasetDto.Builder hbuilder = Mica.HarmonizedDatasetDto.newBuilder();

    if(dataset.hasHarmonizationTable() && !Strings.isNullOrEmpty(dataset.getHarmonizationTable().getStudyId()) &&
      isStudyTablePermitted(asDraft, "harmonization", dataset.getHarmonizationTable().getStudyId())) {
      hbuilder.setHarmonizationTable(
        createHarmonizationLinkDtoFromHarmonizationTable(dataset.getHarmonizationTable(), asDraft));
    }

    if(!dataset.getStudyTables().isEmpty()) {
      dataset.getStudyTables().stream()
        .filter(studyTable -> isStudyTablePermitted(asDraft, "individual", studyTable.getStudyId()))
        .forEach(studyTable -> hbuilder.addStudyTables(asDto(studyTable, studySummary)));
    }

    if(!dataset.getHarmonizationTables().isEmpty()) {
      dataset.getHarmonizationTables().stream()
        .filter(studyTable -> isStudyTablePermitted(asDraft, "harmonization", studyTable.getStudyId()))
        .forEach(harmonizationTable -> hbuilder.addHarmonizationTables(asDto(harmonizationTable, studySummary)));
    }

    builder.setExtension(Mica.HarmonizedDatasetDto.type, hbuilder.build());

    Mica.PermissionsDto permissionsDto = permissionsDtos.asDto(dataset);
    if(asDraft) {
      HarmonizationDatasetState state = harmonizationDatasetStateRepository.findOne(dataset.getId());
      if(state != null) {
        builder.setPublished(state.isPublished());
        builder.setExtension(Mica.EntityStateDto.datasetState,
          entityStateDtos.asDto(state).setPermissions(permissionsDto).build());
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
    if(resolver.hasProject()) {
      builder.setProject(resolver.getProject());
    }
    if(resolver.hasTable()) {
      builder.setTable(resolver.getTable());
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
  Mica.DatasetVariableSummaryDto asSummaryDto(@NotNull DatasetVariable variable, OpalTable opalTable) {
    return asSummaryDto(variable, opalTable, false);
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
        });
    }

    return builder.build();
  }

  @NotNull
  Mica.DatasetVariableSummaryDto asSummaryDto(@NotNull DatasetVariable variable, OpalTable opalTable,
    boolean includeSummaries) {
    Mica.DatasetVariableSummaryDto.Builder builder = Mica.DatasetVariableSummaryDto.newBuilder() //
      .setResolver(asDto(DatasetVariable.IdResolver.from(variable.getId())));

    if(variable.getAttributes() != null) {
      variable.getAttributes().asAttributeList()
        .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }

    if(opalTable instanceof StudyTable) builder.setStudyTable(asDto((StudyTable) opalTable, includeSummaries));
    else if(opalTable instanceof HarmonizationStudyTable) {
      builder.setHarmonizationStudyTable(asDto((HarmonizationStudyTable) opalTable, includeSummaries));
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
    Mica.DatasetDto.StudyTableDto.Builder sbuilder = Mica.DatasetDto.StudyTableDto.newBuilder() //
      .setProject(studyTable.getProject())//
      .setTable(studyTable.getTable()) //
      .setWeight(studyTable.getWeight()) //
      .setStudyId(studyTable.getStudyId()) //
      .setPopulationId(studyTable.getPopulationId()) //
      .setDataCollectionEventId(studyTable.getDataCollectionEventId()) //
      .setDceId(studyTable.getDataCollectionEventUId());

    if(includeSummary) sbuilder.setStudySummary(studySummaryDtos.asDto(studyTable.getStudyId()));

    sbuilder.addAllName(localizedStringDtos.asDto(studyTable.getName()));
    sbuilder.addAllDescription(localizedStringDtos.asDto(studyTable.getDescription()));

    return sbuilder;
  }

  public Mica.DatasetDto.HarmonizationTableDto.Builder asDto(HarmonizationStudyTable harmonizationTable) {
    return asDto(harmonizationTable, false);
  }

  public Mica.DatasetDto.HarmonizationTableDto.Builder asDto(HarmonizationStudyTable harmonizationTable,
    boolean includeSummary) {
    Mica.DatasetDto.HarmonizationTableDto.Builder hBuilder = Mica.DatasetDto.HarmonizationTableDto.newBuilder()
      .setProject(harmonizationTable.getProject())
      .setTable(harmonizationTable.getTable())
      .setWeight(harmonizationTable.getWeight())
      .setStudyId(harmonizationTable.getStudyId())
      .setPopulationId(harmonizationTable.getPopulationId());

    if(includeSummary) hBuilder.setStudySummary(studySummaryDtos.asDto(harmonizationTable.getStudyId()));

    hBuilder.addAllName(localizedStringDtos.asDto(harmonizationTable.getName()));
    hBuilder.addAllDescription(localizedStringDtos.asDto(harmonizationTable.getDescription()));

    return hBuilder;
  }

  public Mica.DatasetVariableAggregationDto.Builder asDto(@NotNull OpalTable opalTable,
    @Nullable Math.SummaryStatisticsDto summary, boolean withStudySummary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();

    if(summary == null) return aggDto;

    if(summary.hasExtension(Math.CategoricalSummaryDto.categorical)) {
      aggDto = asDto(summary.getExtension(Math.CategoricalSummaryDto.categorical));
    } else if(summary.hasExtension(Math.ContinuousSummaryDto.continuous)) {
      aggDto = asDto(summary.getExtension(Math.ContinuousSummaryDto.continuous));
    } else if(summary.hasExtension(Math.DefaultSummaryDto.defaultSummary)) {
      aggDto = asDto(summary.getExtension(Math.DefaultSummaryDto.defaultSummary));
    } else if(summary.hasExtension(Math.TextSummaryDto.textSummary)) {
      aggDto = asDto(summary.getExtension(Math.TextSummaryDto.textSummary));
    } else if(summary.hasExtension(Math.GeoSummaryDto.geoSummary)) {
      aggDto = asDto(summary.getExtension(Math.GeoSummaryDto.geoSummary));
    } else if(summary.hasExtension(Math.BinarySummaryDto.binarySummary)) {
      aggDto = asDto(summary.getExtension(Math.BinarySummaryDto.binarySummary));
    }

    if(opalTable instanceof StudyTable)
      aggDto.setStudyTable(asDto((StudyTable) opalTable, withStudySummary));
    else if (opalTable instanceof HarmonizationStudyTable)
      aggDto.setHarmonizationStudyTable(asDto((HarmonizationStudyTable) opalTable, withStudySummary));

    return aggDto;
  }

  public Mica.DatasetVariableContingencyDto.Builder asContingencyDto(@NotNull OpalTable opalTable,
    DatasetVariable variable, DatasetVariable crossVariable, @Nullable Search.QueryResultDto results) {
    Mica.DatasetVariableContingencyDto.Builder crossDto = Mica.DatasetVariableContingencyDto.newBuilder();

    if(opalTable instanceof StudyTable)
      crossDto.setStudyTable(asDto((StudyTable) opalTable, true));
    else if (opalTable instanceof HarmonizationStudyTable)
      crossDto.setHarmonizationStudyTable(asDto((HarmonizationStudyTable) opalTable));

    Mica.DatasetVariableAggregationDto.Builder allAggBuilder = Mica.DatasetVariableAggregationDto.newBuilder();

    if(results == null) {
      allAggBuilder.setN(0);
      allAggBuilder.setTotal(0);
      crossDto.setAll(allAggBuilder);
      return crossDto;
    }

    allAggBuilder.setTotal(results.getTotalHits());
    MicaConfig micaConfig = micaConfigService.getConfig();
    int privacyThreshold = micaConfig.getPrivacyThreshold();
    crossDto.setPrivacyThreshold(privacyThreshold);
    boolean privacyChecks = crossVariable.hasCategories() ? validatePrivacyThreshold(results, privacyThreshold) : true;
    boolean totalPrivacyChecks = validateTotalPrivacyThreshold(results, privacyThreshold);

    // add facet results in the same order as the variable categories
    List<String> catNames = variable.getValueType().equals(BooleanType.get().getName()) ?
      Lists.newArrayList("true", "false") : variable.getCategories().stream().map(DatasetCategory::getName).collect(Collectors.toList());
    catNames.forEach(catName -> results.getFacetsList().stream()
      .filter(facet -> facet.hasFacet() && catName.equals(facet.getFacet())).forEach(facet -> {
        boolean privacyCheck = privacyChecks && checkPrivacyThreshold(facet.getFilters(0).getCount(), privacyThreshold);
        Mica.DatasetVariableAggregationDto.Builder aggBuilder = Mica.DatasetVariableAggregationDto.newBuilder();
        aggBuilder.setTotal(totalPrivacyChecks ? results.getTotalHits() : 0);
        aggBuilder.setTerm(facet.getFacet());
        DatasetCategory category = variable.getCategory(facet.getFacet());
        aggBuilder.setMissing(category != null && category.isMissing());
        addSummaryStatistics(crossVariable, aggBuilder, facet, privacyCheck, totalPrivacyChecks);
        crossDto.addAggregations(aggBuilder);
      }));

    // add total facet for all variable categories
    results.getFacetsList().stream().filter(facet -> facet.hasFacet() && "_total".equals(facet.getFacet()))
      .forEach(facet -> {
        boolean privacyCheck = privacyChecks && facet.getFilters(0).getCount() > micaConfig.getPrivacyThreshold();
        addSummaryStatistics(crossVariable, allAggBuilder, facet, privacyCheck, totalPrivacyChecks);
      });

    crossDto.setAll(allAggBuilder);

    return crossDto;
  }

  private boolean checkPrivacyThreshold(int count, int threshold) {
    return count == 0 || count >= threshold;
  }

  private boolean validateTotalPrivacyThreshold(Search.QueryResultDtoOrBuilder results, int privacyThreshold) {
    return results.getFacetsList().stream()
      .allMatch(facet -> checkPrivacyThreshold(facet.getFilters(0).getCount(), privacyThreshold));
  }

  private boolean validatePrivacyThreshold(Search.QueryResultDtoOrBuilder results, int privacyThreshold) {
    return results.getFacetsList().stream().map(Search.FacetResultDto::getFrequenciesList).flatMap(Collection::stream)
      .allMatch(freq -> checkPrivacyThreshold(freq.getCount(), privacyThreshold));
  }

  private void addSummaryStatistics(DatasetVariable crossVariable,
    Mica.DatasetVariableAggregationDto.Builder aggBuilder, Search.FacetResultDto facet, boolean privacyCheck,
    boolean totalPrivacyCheck) {
    aggBuilder.setN(totalPrivacyCheck ? facet.getFilters(0).getCount() : -1);
    if(!privacyCheck) return;

    List<String> catNames = crossVariable.getValueType().equals(BooleanType.get().getName()) ?
      Lists.newArrayList("1", "0") :
      (crossVariable.hasCategories() ? crossVariable.getCategories().stream().map(DatasetCategory::getName).collect(Collectors.toList()) : Lists.newArrayList());
    // order results as the order of cross variable categories
    catNames.forEach(catName -> facet.getFrequenciesList().stream().filter(freq -> catName.equals(freq.getTerm()))
        .forEach(freq -> aggBuilder.addFrequencies(asDto(crossVariable, freq))));
    // observed terms, not described by categories
    facet.getFrequenciesList().stream().filter(freq -> !catNames.contains(freq.getTerm()))
      .forEach(freq -> aggBuilder.addFrequencies(asDto(crossVariable, freq)));

    if(facet.hasStatistics()) {
      aggBuilder.setStatistics(asDto(facet.getStatistics()));
    }
  }

  private Mica.FrequencyDto.Builder asDto(DatasetVariable crossVariable,
    Search.FacetResultDto.TermFrequencyResultDto result) {
    if (crossVariable.getValueType().equals(BooleanType.get().getName())) {
      // for some reason 0/1 is returned instead of false/true
      return Mica.FrequencyDto.newBuilder()
        .setValue("1".equals(result.getTerm()) ? "true" : "false")
        .setCount(result.getCount())
        .setMissing(false);
    } else if (crossVariable.getCategory(result.getTerm()) != null) {
      DatasetCategory category = crossVariable.getCategory(result.getTerm());
      return Mica.FrequencyDto.newBuilder()
        .setValue(result.getTerm())
        .setCount(result.getCount())
        .setMissing(category != null && category.isMissing());
    } else {
      // observed value, not described by a category
      return Mica.FrequencyDto.newBuilder()
        .setValue(result.getTerm())
        .setCount(result.getCount())
        .setMissing(false);
    }
  }

  private Mica.StatisticsDto.Builder asDto(Search.FacetResultDto.StatisticalResultDto result) {
    return Mica.StatisticsDto.newBuilder() //
      .setMin(result.getMin()) //
      .setMax(result.getMax()) //
      .setMean(result.getMean()) //
      .setSum(result.getTotal()) //
      .setSumOfSquares(result.getSumOfSquares()) //
      .setVariance(result.getVariance()) //
      .setStdDeviation(result.getStdDeviation());
  }

  private Mica.DatasetVariableAggregationDto.Builder asDto(Math.CategoricalSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList(),
      summary.hasOtherFrequency() ? Long.valueOf(summary.getOtherFrequency()).intValue() : 0);
    return aggDto;
  }

  private Mica.DatasetVariableAggregationDto.Builder asDto(Math.DefaultSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList());
    return aggDto;
  }

  private Mica.DatasetVariableAggregationDto.Builder asDto(Math.TextSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList(),
      summary.hasOtherFrequency() ? Long.valueOf(summary.getOtherFrequency()).intValue() : 0);
    return aggDto;
  }

  private Mica.DatasetVariableAggregationDto.Builder asDto(Math.GeoSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList());
    return aggDto;
  }

  private Mica.DatasetVariableAggregationDto.Builder asDto(Math.BinarySummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList());
    return aggDto;
  }

  private Mica.FrequencyDto.Builder asDto(Math.FrequencyDto freq) {
    return Mica.FrequencyDto.newBuilder().setValue(freq.getValue()).setCount(Long.valueOf(freq.getFreq()).intValue())
      .setMissing(freq.getMissing());
  }

  private Mica.IntervalFrequencyDto.Builder asDto(Math.IntervalFrequencyDto inter) {
    return Mica.IntervalFrequencyDto.newBuilder().setCount((int)inter.getFreq())
      .setLower(inter.getLower()).setUpper(inter.getUpper());
  }

  private void addFrequenciesDto(Mica.DatasetVariableAggregationDto.Builder aggDto,
    List<Math.FrequencyDto> frequencies) {
    addFrequenciesDto(aggDto, frequencies, 0);
  }

  private void addFrequenciesDto(Mica.DatasetVariableAggregationDto.Builder aggDto, List<Math.FrequencyDto> frequencies,
    int otherFrequency) {
    int n = otherFrequency;
    if(frequencies != null) {
      for(Math.FrequencyDto freq : frequencies) {
        aggDto.addFrequencies(asDto(freq));
        if(!freq.getMissing()) n += freq.getFreq();
      }
    }
    if (otherFrequency>0)
      aggDto.addFrequencies(Mica.FrequencyDto.newBuilder().setValue("???").setCount(otherFrequency)
        .setMissing(false));
    aggDto.setN(n);
  }

  private Mica.DatasetVariableAggregationDto.Builder asDto(Math.ContinuousSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    Math.DescriptiveStatsDto stats = summary.getSummary();

    aggDto.setN(Long.valueOf(stats.getN()).intValue());

    Mica.StatisticsDto.Builder builder = Mica.StatisticsDto.newBuilder();

    if(stats.hasSum()) builder.setSum(Double.valueOf(stats.getSum()).floatValue());
    if(stats.hasMin() && stats.getMin() != Double.POSITIVE_INFINITY)
      builder.setMin(Double.valueOf(stats.getMin()).floatValue());
    if(stats.hasMax() && stats.getMax() != Double.NEGATIVE_INFINITY)
      builder.setMax(Double.valueOf(stats.getMax()).floatValue());
    if(stats.hasMean() && !Double.isNaN(stats.getMean())) builder.setMean(Double.valueOf(stats.getMean()).floatValue());
    if(stats.hasSumsq() && !Double.isNaN(stats.getSumsq()))
      builder.setSumOfSquares(Double.valueOf(stats.getSumsq()).floatValue());
    if(stats.hasVariance() && !Double.isNaN(stats.getVariance()))
      builder.setVariance(Double.valueOf(stats.getVariance()).floatValue());
    if(stats.hasStdDev() && !Double.isNaN(stats.getStdDev()))
      builder.setStdDeviation(Double.valueOf(stats.getStdDev()).floatValue());

    aggDto.setStatistics(builder);

    if(summary.getFrequenciesCount() > 0) {
      summary.getFrequenciesList().forEach(freq -> aggDto.addFrequencies(asDto(freq)));
    }

    if (summary.getIntervalFrequencyCount() > 0) {
      summary.getIntervalFrequencyList().forEach(inter -> aggDto.addIntervalFrequencies(asDto(inter)));
    }

    int total = 0;
    if(summary.getFrequenciesCount() > 0) {
      for(Math.FrequencyDto freq : summary.getFrequenciesList()) {
        total += freq.getFreq();
      }
    }
    aggDto.setTotal(total);

    return aggDto;
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
  public Dataset fromDto(Mica.DatasetDtoOrBuilder dto) {
    Dataset dataset = dto.hasExtension(Mica.HarmonizedDatasetDto.type)
      ? fromDto(dto.getExtension(Mica.HarmonizedDatasetDto.type))
      : dto.hasExtension(Mica.CollectedDatasetDto.type)
        ? fromDto(dto.getExtension(Mica.CollectedDatasetDto.type))
        : new StudyDataset();

    if(dto.hasId()) dataset.setId(dto.getId());
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
      harmonizationLink.setProject(dto.getHarmonizationTable().getProject());
      harmonizationLink.setTable(dto.getHarmonizationTable().getTable());
      harmonizationLink.setStudyId(dto.getHarmonizationTable().getStudyId());
      harmonizationLink.setPopulationId(dto.getHarmonizationTable().getPopulationId());
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
    table.setPopulationId(dto.getPopulationId());
    table.setDataCollectionEventId(dto.getDataCollectionEventId());
    table.setProject(dto.getProject());
    table.setTable(dto.getTable());
    table.setWeight(dto.getWeight());

    table.setName(localizedStringDtos.fromDto(dto.getNameList()));
    table.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));

    return table;
  }

  private HarmonizationStudyTable fromDto(Mica.DatasetDto.HarmonizationTableDto dto) {
    HarmonizationStudyTable table = new HarmonizationStudyTable();
    table.setStudyId(dto.getStudyId());
    table.setPopulationId(dto.getPopulationId());
    table.setPopulationId(dto.getPopulationId());
    table.setProject(dto.getProject());
    table.setTable(dto.getTable());
    table.setWeight(dto.getWeight());

    table.setName(localizedStringDtos.fromDto(dto.getNameList()));
    table.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));

    return table;
  }

  private Mica.DatasetDto.HarmonizationTableDto.Builder createHarmonizationLinkDtoFromHarmonizationTable(
    HarmonizationStudyTable harmonizationLink, boolean asDraft) {
    Mica.DatasetDto.HarmonizationTableDto.Builder harmonizationLinkBuilder = Mica.DatasetDto.HarmonizationTableDto
      .newBuilder();

    if(!Strings.isNullOrEmpty(harmonizationLink.getProject()))
      harmonizationLinkBuilder.setProject(harmonizationLink.getProject());

    if(!Strings.isNullOrEmpty(harmonizationLink.getTable()))
      harmonizationLinkBuilder.setTable(harmonizationLink.getTable());

    String studyId = harmonizationLink.getStudyId();

    if(asDraft && studyId != null) {
      harmonizationLinkBuilder.setStudyId(harmonizationLink.getStudyId());
      harmonizationLinkBuilder.setPopulationId(harmonizationLink.getPopulationId());
    } else if(!asDraft && studyId != null && publishedStudyService.findById(studyId) != null) {
      harmonizationLinkBuilder.setStudyId(harmonizationLink.getStudyId());
      harmonizationLinkBuilder.setPopulationId(harmonizationLink.getPopulationId());
    } else {
      harmonizationLinkBuilder.setStudyId("-");
      harmonizationLinkBuilder.setPopulationId("-");
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
