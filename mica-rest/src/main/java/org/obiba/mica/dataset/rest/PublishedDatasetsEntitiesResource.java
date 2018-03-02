/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.rest.rql.RQLCriteriaOpalConverter;
import org.obiba.mica.dataset.rest.rql.RQLCriterionOpalConverter;
import org.obiba.mica.dataset.rest.rql.RQLFieldReferences;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.web.model.DocumentDigestDtos;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.web.model.Search;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Path("/datasets/entities")
@Scope("request")
@RequiresAuthentication
public class PublishedDatasetsEntitiesResource {

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private OpalService opalService;

  @Inject
  private DocumentDigestDtos documentDigestDtos;

  @GET
  @Path("_count")
  @Timed
  public MicaSearch.EntitiesCountDto countEntities(@QueryParam("query") String query, @QueryParam("type") @DefaultValue("Participant") String entityType) {
    RQLCriteriaOpalConverter converter = applicationContext.getBean(RQLCriteriaOpalConverter.class);
    converter.parse(query);
    Map<BaseStudy, List<RQLCriterionOpalConverter>> studyConverters = converter.getCriterionConverters().stream()
        .collect(Collectors.groupingBy(c -> c.getVariableReferences().getStudy()));
    List<StudyEntitiesQuery> studyEntitiesQueries = studyConverters.keySet().stream()
        .map(study -> new StudyEntitiesQuery(entityType, study, studyConverters.get(study)))
        .collect(Collectors.toList());

    MicaSearch.EntitiesCountDto.Builder builder = MicaSearch.EntitiesCountDto.newBuilder()
        .setQuery(query);

    int total = 0;
    for (StudyEntitiesQuery studyEntitiesQuery : studyEntitiesQueries) {
      total = total + studyEntitiesQuery.getTotal();
      builder.addCounts(studyEntitiesQuery.getStudyEntitiesCount());
    }
    // sum of all the study counts because entities are study specific
    builder.setTotal(total);

    return builder.build();
  }

  private class StudyEntitiesQuery {
    private final BaseStudy study;
    private final String entityType;
    private final String opalUrl;
    private final List<RQLCriterionOpalConverter> opalConverters;
    private final Search.EntitiesResultDto opalResults;

    private StudyEntitiesQuery(String entityType, BaseStudy study, List<RQLCriterionOpalConverter> opalConverters) {
      this.entityType = entityType;
      this.study = study;
      this.opalUrl = RQLFieldReferences.getOpal(study);
      this.opalConverters = opalConverters;
      this.opalResults = opalService.getEntitiesCount(opalUrl, getOpalQuery(), entityType);
    }

    private String getOpalQuery() {
      return opalConverters.stream().map(RQLCriterionOpalConverter::getOpalQuery).collect(Collectors.joining(","));
    }

    private String getMicaQuery() {
      return opalConverters.stream().map(RQLCriterionOpalConverter::getMicaQuery).collect(Collectors.joining(","));
    }

    private int getTotal() {
      return opalResults.getTotalHits();
    }

    public String getEntityType() {
      return entityType;
    }

    public MicaSearch.StudyEntitiesCountDto getStudyEntitiesCount() {
      MicaSearch.StudyEntitiesCountDto.Builder builder = MicaSearch.StudyEntitiesCountDto.newBuilder()
        .setTotal(getTotal())
        .setEntityType(getEntityType())
        .setQuery(getMicaQuery())
        .setStudy(documentDigestDtos.asDto(study));

      if (opalResults.getPartialResultsCount()>0) {
        Map<Dataset, List<Search.EntitiesResultDto>> datasetResults = opalResults.getPartialResultsList().stream()
          .collect(Collectors.groupingBy(res -> findConverter(res.getQuery()).getVariableReferences().getDataset()));
        datasetResults.keySet()
          .forEach(ds -> builder.addCounts(createDatasetEntitiesCount(ds, datasetResults.get(ds))));
      } else
        builder.addCounts(createDatasetEntitiesCount(opalResults));

      return builder.build();
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
      builder.setQuery(converter.getMicaQuery())
        .setCount(opalResult.getTotalHits())
        .setVariable(documentDigestDtos.asDto(converter.getVariableReferences().getVariable()));
      return builder.build();
    }

    private RQLCriterionOpalConverter findConverter(String opalQuery) {
      return opalConverters.stream().filter(c -> c.getOpalQuery().equals(opalQuery)).findFirst().get();
    }

  }
}
