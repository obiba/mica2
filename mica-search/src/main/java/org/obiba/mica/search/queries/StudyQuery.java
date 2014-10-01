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

import javax.inject.Inject;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.search.CountStatsDtoBuilders;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.search.StudyIndexer;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class StudyQuery extends AbstractDocumentQuery {

  private static final String STUDY_FACETS_YML = "study-facets.yml";

  @Inject
  PublishedStudyService publishedStudyService;

  @Inject
  Dtos dtos;

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
  public void processHits(MicaSearch.QueryResultDto.Builder builder, SearchHits hits, CountStatsData counts) {
    if(counts == null) {
      processHits(builder, hits);
      return;
    }

    MicaSearch.StudyResultDto.Builder resBuilder = MicaSearch.StudyResultDto.newBuilder();
    CountStatsDtoBuilders.StudyCountStatsBuilder studyCountStatsBuilder = CountStatsDtoBuilders.StudyCountStatsBuilder
        .newBuilder(counts);

    for(SearchHit hit : hits) {
      Study study = publishedStudyService.findById(hit.getId());
      resBuilder.addSummaries(dtos.asSummaryDtoBuilder(study)
          .setExtension(MicaSearch.CountStatsDto.studyCountStats, studyCountStatsBuilder.build(study)).build());

    }

    builder.setExtension(MicaSearch.StudyResultDto.result, resBuilder.build());
  }

  private void processHits(MicaSearch.QueryResultDto.Builder builder, SearchHits hits) {
    MicaSearch.StudyResultDto.Builder resBuilder = MicaSearch.StudyResultDto.newBuilder();
    for(SearchHit hit : hits) {
      resBuilder.addSummaries(dtos.asSummaryDto(publishedStudyService.findById(hit.getId())));
    }
    builder.setExtension(MicaSearch.StudyResultDto.result, resBuilder.build());
  }

  @Override
  protected Resource getAggregationsDescription() {
    return new ClassPathResource(STUDY_FACETS_YML);
  }

  @Override
  public Map<String, Integer> getStudyCounts() {
    return getStudyCounts(JOIN_FIELD);
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(JOIN_FIELD);
  }
}
