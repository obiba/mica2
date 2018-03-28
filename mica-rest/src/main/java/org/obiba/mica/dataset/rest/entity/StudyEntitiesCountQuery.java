/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.entity;

import com.google.common.base.Strings;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.rest.entity.rql.RQLCriterionOpalConverter;
import org.obiba.mica.dataset.rest.entity.rql.RQLFieldReferences;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.DocumentDigestDtos;
import org.obiba.mica.web.model.LocalizedStringDtos;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.web.model.Search;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StudyEntitiesCountQuery {

  private final OpalService opalService;

  private final StudyService studyService;

  private final int privacyThreshold;

  private final DocumentDigestDtos documentDigestDtos;

  private final LocalizedStringDtos localizedStringDtos;

  private final BaseStudy study;

  private final String entityType;

  private final List<RQLCriterionOpalConverter> opalConverters;

  private final Search.EntitiesResultDto opalResults;

  StudyEntitiesCountQuery(StudyService studyService, OpalService opalService, int privacyThreshold, DocumentDigestDtos documentDigestDtos, LocalizedStringDtos localizedStringDtos,
                          String entityType, BaseStudy study, List<RQLCriterionOpalConverter> opalConverters) {
    this.studyService = studyService;
    this.opalService = opalService;
    this.privacyThreshold = privacyThreshold;
    this.documentDigestDtos = documentDigestDtos;
    this.localizedStringDtos = localizedStringDtos;
    this.entityType = entityType;
    this.study = study;
    this.opalConverters = opalConverters;
    if (study instanceof HarmonizationStudy) {
      this.opalResults = queryHarmonizationStudy();
    } else {
      this.opalResults = queryIndividualStudy();
    }
  }

  public int getTotal() {
    return opalResults.getTotalHits();
  }

  public String getEntityType() {
    return entityType;
  }

  public MicaSearch.StudyEntitiesCountDto getStudyEntitiesCount() {
    int total = getTotal();
    MicaSearch.StudyEntitiesCountDto.Builder builder = MicaSearch.StudyEntitiesCountDto.newBuilder()
      .setTotal(total < privacyThreshold ? privacyThreshold : total)
      .setBelowPrivacyThreshold(total < privacyThreshold)
      .setEntityType(getEntityType())
      .setQuery(getMicaQuery())
      .setStudy(documentDigestDtos.asDto(study));

    if (opalResults.getPartialResultsCount() > 0) {
      Map<Dataset, List<Search.EntitiesResultDto>> datasetResults = opalResults.getPartialResultsList().stream()
        .collect(Collectors.groupingBy(res -> findConverter(res.getQuery()).getVariableReferences().getDataset()));
      datasetResults.keySet()
        .forEach(ds -> builder.addCounts(createDatasetEntitiesCount(ds, datasetResults.get(ds))));
    } else
      builder.addCounts(createDatasetEntitiesCount(opalResults));

    return builder.build();
  }

  //
  // Private methods
  //

  private Search.EntitiesResultDto queryIndividualStudy() {
    return this.opalService.getEntitiesCount(getOpalUrl(study), getOpalQuery(), entityType);
  }

  // TODO finalize result processing
  private Search.EntitiesResultDto queryHarmonizationStudy() {
    List<Search.EntitiesResultDto> results = opalConverters.get(0).getStudyTables().parallelStream()
      .map(studyTable -> {
        BaseStudy studyTableStudy = studyService.findStudy(studyTable.getStudyId());
        String opalUrl = getOpalUrl(studyTableStudy);
        String opalQuery = getOpalQuery(studyTable);
        return this.opalService.getEntitiesCount(opalUrl, opalQuery, entityType);
      }).collect(Collectors.toList());
    // TODO merge results
    return results.get(0);
  }

  private String getOpalUrl(BaseStudy study) {
    return Strings.isNullOrEmpty(study.getOpal()) ? "_default" : study.getOpal();
  }

  private String getOpalQuery(BaseStudyTable studyTable) {
    return opalConverters.stream().map(c -> c.getOpalQuery(studyTable)).collect(Collectors.joining(","));
  }

  private String getOpalQuery() {
    return opalConverters.stream().map(RQLCriterionOpalConverter::getOpalQuery).collect(Collectors.joining(","));
  }

  private String getMicaQuery() {
    return opalConverters.stream().map(RQLCriterionOpalConverter::getMicaQuery).collect(Collectors.joining(","));
  }

  private MicaSearch.DatasetEntitiesCountDto createDatasetEntitiesCount(Search.EntitiesResultDto opalResult) {
    MicaSearch.DatasetEntitiesCountDto.Builder builder = MicaSearch.DatasetEntitiesCountDto.newBuilder();
    RQLCriterionOpalConverter converter = findConverter(opalResult.getQuery());
    builder.setDataset(documentDigestDtos.asDto(converter.getVariableReferences().getDataset()));
    builder.addCounts(createVariableEntitiesCount(opalResult));
    return builder.build();
  }

  private MicaSearch.DatasetEntitiesCountDto createDatasetEntitiesCount(Dataset dataset, List<Search.EntitiesResultDto> opalResults) {
    MicaSearch.DatasetEntitiesCountDto.Builder builder = MicaSearch.DatasetEntitiesCountDto.newBuilder()
      .setDataset(documentDigestDtos.asDto(dataset));
    builder.addAllCounts(opalResults.stream()
      .map(this::createVariableEntitiesCount)
      .sorted(Comparator.comparing(dto -> dto.getVariable().getId()))
      .collect(Collectors.toList()));
    return builder.build();
  }

  private MicaSearch.VariableEntitiesCountDto createVariableEntitiesCount(Search.EntitiesResultDto opalResult) {
    MicaSearch.VariableEntitiesCountDto.Builder builder = MicaSearch.VariableEntitiesCountDto.newBuilder();
    RQLCriterionOpalConverter converter = findConverter(opalResult.getQuery());
    RQLFieldReferences references = converter.getVariableReferences();
    builder.setQuery(converter.getMicaQuery())
      .setCount(opalResult.getTotalHits())
      .setVariable(documentDigestDtos.asDto(references.getVariable()));
    if (references.hasStudyTableName())
      builder.addAllStudyTableName(localizedStringDtos.asDto(references.getStudyTableName()));
    return builder.build();
  }

  private RQLCriterionOpalConverter findConverter(String opalQuery) {
    return opalConverters.stream().filter(c -> c.getOpalQuery().equals(opalQuery)).findFirst().get();
  }

}
