/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.variable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.json.JSONException;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.search.VariableIndexer;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.search.rest.AbstractSearchResource;
import org.obiba.mica.search.rest.QueryDtoParser;
import org.obiba.mica.service.HarmonizationDatasetService;
import org.obiba.mica.service.StudyDatasetService;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

/**
 * Search for variables in the published variable index.
 */
@Path("/variables/_search")
@RequiresAuthentication
public class PublishedDatasetVariablesSearchResource extends AbstractSearchResource {

  private static final String VARIABLE_FACETS_YML = "variable-facets.yml";

  private static final String AGG_STUDY_IDS = "studyIds";

  private static final String AGG_DATASET_ID = "datasetId";

  @Inject
  PublishedStudyService publishedStudyService;

  @Inject
  PublishedDatasetService publishedDatasetService;

  @Inject
  private Dtos dtos;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Override
  protected String getSearchIndex() {
    return VariableIndexer.PUBLISHED_VARIABLE_INDEX;
  }

  @Override
  protected String getSearchType() {
    return VariableIndexer.VARIABLE_TYPE;
  }

  @GET
  @Timed
  public MicaSearch.QueryResultDto list(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("size") @DefaultValue("10") int size, @QueryParam("detailed") @DefaultValue("false") boolean detailed)
      throws JSONException, IOException {

    return execute(QueryBuilders.matchAllQuery(), from, size, true, detailed);
  }

  @POST
  @Timed
  public MicaSearch.QueryResultDto list(MicaSearch.QueryDto dtoQuery) throws IOException {
    return execute(QueryDtoParser.newParser().parse(dtoQuery), dtoQuery.getFrom(), dtoQuery.getSize(), true,
        dtoQuery.getDetailed());
  }

  @Override
  protected Resource getAggregationsDescription() {
    return new ClassPathResource(VARIABLE_FACETS_YML);
  }

  @Override
  protected void processHits(MicaSearch.QueryResultDto.Builder builder, boolean detailedQuery, boolean detailedResult,
      SearchHits hits) throws IOException {
    MicaSearch.DatasetVariableResultDto.Builder resBuilder = MicaSearch.DatasetVariableResultDto.newBuilder();
    Map<String, Study> studyMap = Maps.newHashMap();

    for(SearchHit hit : hits) {
      DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(hit.getId());

      if(detailedQuery) {
        InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
        DatasetVariable variable = objectMapper.readValue(inputStream, DatasetVariable.class);
        resBuilder.addSummaries(processHit(resolver, variable, studyMap));
        if(detailedResult) {
          resBuilder.addVariables(dtos.asDto(variable));
        }
      }
    }
    builder.setExtension(MicaSearch.DatasetVariableResultDto.result, resBuilder.build());
  }

  @Override
  protected void processAggregations(MicaSearch.QueryResultDto.Builder builder, Aggregations aggregations) {
    super.processAggregations(builder, aggregations);
    // easier to deal with DTOs
    builder.getAggsList().stream().filter(aggDto -> aggDto.getAggregation().matches(AGG_DATASET_ID+"|"+AGG_STUDY_IDS))
        .collect(Collectors.toList()).forEach(agg -> {
      processAggregation(builder, agg);
    });
  }

  protected void processAggregation(MicaSearch.QueryResultDto.Builder builder, MicaSearch.AggregationResultDto agg) {
    List<MicaSearch.TermsAggregationResultDto> terms = agg.getExtension(MicaSearch.TermsAggregationResultDto.terms);
    if(terms == null) return;

    switch(agg.getAggregation()) {
      case AGG_DATASET_ID:
        List<Mica.DatasetDto> datasetDtos = publishedDatasetService
            .findByIds(terms.stream().map(term -> term.getKey()).collect(Collectors.toList())).stream()
            .map(dataset -> dtos.asDto(dataset)).collect(Collectors.toList());
        builder.setExtension(MicaSearch.DatasetsResultDto.result,
            MicaSearch.DatasetsResultDto.newBuilder().addAllDatasets(datasetDtos).build());
        break;
      case AGG_STUDY_IDS:
        List<Mica.StudySummaryDto> studySummaryDtos = publishedStudyService
            .findByIds(terms.stream().map(term -> term.getKey()).collect(Collectors.toList())).stream()
            .map(study -> dtos.asSummaryDto(study)).collect(Collectors.toList());
        builder.setExtension(MicaSearch.StudySummariesResultDto.result,
            MicaSearch.StudySummariesResultDto.newBuilder().addAllSummaries(studySummaryDtos).build());
        break;
    }
  }

  /**
   * Fill the variable resolver dto with user-friendly labels about the variable, the dataset and the study.
   *
   * @param resolver
   * @param variable
   * @param studyMap study cache
   * @return
   */
  private Mica.DatasetVariableResolverDto processHit(DatasetVariable.IdResolver resolver, DatasetVariable variable,
      Map<String, Study> studyMap) {
    Mica.DatasetVariableResolverDto.Builder builder = dtos.asDto(resolver);

    String studyId = resolver.hasStudyId() ? resolver.getStudyId() : null;

    if(resolver.getType().equals(DatasetVariable.Type.Study) ||
        resolver.getType().equals(DatasetVariable.Type.Harmonized)) {
      studyId = variable.getStudyIds().get(0);
    }

    if(studyId != null) {
      builder.setStudyId(studyId);
      try {
        Study study;
        if(studyMap.containsKey(studyId)) {
          study = studyMap.get(studyId);
        } else {
          study = publishedStudyService.findById(studyId);
          studyMap.put(studyId, study);
        }

        builder.addAllStudyName(dtos.asDto(study.getName()));
        builder.addAllStudyAcronym(dtos.asDto(study.getAcronym()));
      } catch(NoSuchStudyException e) {
      }
    }

    builder.addAllDatasetName(dtos.asDto(variable.getDatasetName()));

    if(variable.hasAttribute("label", null)) {
      builder.addAllVariableLabel(dtos.asDto(variable.getAttributes().getAttribute("label", null).getValues()));
    }

    return builder.build();
  }

}
