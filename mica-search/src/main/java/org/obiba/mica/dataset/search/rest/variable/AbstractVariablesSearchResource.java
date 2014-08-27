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
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.json.JSONException;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.search.rest.AbstractSearchResource;
import org.obiba.mica.search.rest.QueryDtoParser;
import org.obiba.mica.service.HarmonizationDatasetService;
import org.obiba.mica.service.StudyDatasetService;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.StudyService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import static org.obiba.mica.web.model.MicaSearch.QueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

public abstract class AbstractVariablesSearchResource extends AbstractSearchResource {

  private static final String VARIABLE_FACETS_YML = "variable-facets.yml";

  @Inject
  private Dtos dtos;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Inject
  private StudyService studyService;

  @GET
  @Timed
  public QueryResultDto list(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("size") @DefaultValue("10") int size, @QueryParam("detailed") @DefaultValue("false") boolean detailed)
      throws JSONException, IOException {

    return execute(QueryBuilders.matchAllQuery(), from, size, true, detailed);
  }

  @POST
  @Timed
  public QueryResultDto list(QueryDto dtoQuery) throws IOException {
    return execute(QueryDtoParser.newParser().parse(dtoQuery), dtoQuery.getFrom(), dtoQuery.getSize(), true,
        dtoQuery.getDetailed());
  }

  @Override
  protected Resource getAggregationsDescription() {
    return new ClassPathResource(VARIABLE_FACETS_YML);
  }

  @Override
  protected void processHits(QueryResultDto.Builder builder, boolean detailedQuery, boolean detailedResult, SearchHits hits) throws IOException {
    MicaSearch.DatasetVariableResultDto.Builder resBuilder = MicaSearch.DatasetVariableResultDto.newBuilder();
    Map<String, Study> studyMap = Maps.newHashMap();

    for(SearchHit hit : hits) {
      DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(hit.getId());

      if (detailedQuery) {
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

  /**
   * Fill the variable resolver dto with user-friendly labels about the variable, the dataset and the study.
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
        if (studyMap.containsKey(studyId)) {
          study = studyMap.get(studyId);
        } else {
         study =  studyService.findPublishedStudy(studyId);
          studyMap.put(studyId, study);
        }

        builder.addAllStudyName(dtos.asDto(study.getName()));
        builder.addAllStudyAcronym(dtos.asDto(study.getAcronym()));
      } catch(NoSuchStudyException e) {
      }
    }

    builder.addAllDatasetName(dtos.asDto(variable.getDatasetName()));

    if (variable.hasAttribute("label", null)) {
      builder.addAllVariableLabel(dtos.asDto(variable.getAttributes().getAttribute("label", null).getValues()));
    }

    return builder.build();
  }
}
