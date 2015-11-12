/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.elasticsearch.search.SearchHits;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.search.aggregations.AggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.StudyTaxonomyMetaDataProvider;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.search.StudyIndexer;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import static org.obiba.mica.search.CountStatsDtoBuilders.StudyCountStatsBuilder;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;
import static org.obiba.mica.web.model.MicaSearch.StudyResultDto;

@Component
@Scope("request")
public class StudyQuery extends AbstractDocumentQuery {

  private static final String STUDY_FACETS_YML = "study-facets.yml";

  @Inject
  PublishedStudyService publishedStudyService;

  @Inject
  Dtos dtos;

  @Inject
  private StudyTaxonomyMetaDataProvider studyTaxonomyMetaDataProvider;

  private static final String JOIN_FIELD = "id";

  @Override
  public String getSearchIndex() {
    return StudyIndexer.PUBLISHED_STUDY_INDEX;
  }

  @Override
  public String getSearchType() {
    return StudyIndexer.STUDY_TYPE;
  }

  @Override
  public Stream<String> getLocalizedQueryStringFields() {
    return Stream.of(StudyIndexer.LOCALIZED_ANALYZED_FIELDS);
  }

  @Override
  protected List<AggregationMetaDataProvider> getAggregationMetaDataProviders() {
    return Arrays.asList(studyTaxonomyMetaDataProvider);
  }

  @Override
  public void processHits(QueryResultDto.Builder builder, SearchHits hits, Scope scope, CountStatsData counts) {
    StudyResultDto.Builder resBuilder = StudyResultDto.newBuilder();
    StudyCountStatsBuilder studyCountStatsBuilder = counts == null ? null : StudyCountStatsBuilder.newBuilder(counts);

    Consumer<Study> addDto = getStudyConsumer(scope, resBuilder, studyCountStatsBuilder);
    List<Study> publishedStudies = publishedStudyService
      .findByIds(Stream.of(hits.hits()).map(h -> h.getId()).collect(Collectors.toList()));
    publishedStudies.forEach(addDto::accept);
    builder.setExtension(StudyResultDto.result, resBuilder.build());
  }

  private Consumer<Study> getStudyConsumer(Scope scope, StudyResultDto.Builder resBuilder,
    StudyCountStatsBuilder studyCountStatsBuilder) {

    return scope == Scope.DETAIL ? (study) -> {
      Mica.StudySummaryDto.Builder summaryBuilder = dtos.asSummaryDtoBuilder(study);
      if(mode == Mode.LIST) {
        summaryBuilder.clearPopulationSummaries();
      }
      if(studyCountStatsBuilder != null) {
        summaryBuilder.setExtension(MicaSearch.CountStatsDto.studyCountStats, studyCountStatsBuilder.build(study))
          .build();
      }
      resBuilder.addSummaries(summaryBuilder.build());
    } : (study) -> resBuilder.addDigests(dtos.asDigestDtoBuilder(study).build());
  }

  @Override
  protected Resource getAggregationsDescription() {
    return new ClassPathResource(STUDY_FACETS_YML);
  }

  @Override
  public Map<String, Integer> getStudyCounts() {
    return getDocumentCounts(JOIN_FIELD);
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(JOIN_FIELD);
  }
}
