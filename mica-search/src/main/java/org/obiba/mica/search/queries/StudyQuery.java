/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import com.google.common.collect.Lists;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.StudiesSetsAggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.StudyTaxonomyMetaDataProvider;
import org.obiba.mica.spi.search.*;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.obiba.mica.search.CountStatsDtoBuilders.StudyCountStatsBuilder;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;
import static org.obiba.mica.web.model.MicaSearch.StudyResultDto;

@Component
@Scope("request")
public class StudyQuery extends AbstractDocumentQuery {

  private final IndividualStudyService individualStudyService;

  private final HarmonizationStudyService harmonizationStudyService;

  private final Dtos dtos;

  private final StudyTaxonomyMetaDataProvider studyTaxonomyMetaDataProvider;

  private final StudiesSetsAggregationMetaDataProvider studiesSetsAggregationMetaDataProvider;

  private static final String JOIN_FIELD = "id";

  @Inject
  public StudyQuery(IndividualStudyService individualStudyService, HarmonizationStudyService harmonizationStudyService, Dtos dtos, StudyTaxonomyMetaDataProvider studyTaxonomyMetaDataProvider, StudiesSetsAggregationMetaDataProvider studiesSetsAggregationMetaDataProvider) {
    this.individualStudyService = individualStudyService;
    this.harmonizationStudyService = harmonizationStudyService;
    this.dtos = dtos;
    this.studyTaxonomyMetaDataProvider = studyTaxonomyMetaDataProvider;
    this.studiesSetsAggregationMetaDataProvider = studiesSetsAggregationMetaDataProvider;
  }

  @Override
  public String getSearchIndex() {
    return Indexer.PUBLISHED_STUDY_INDEX;
  }

  @Override
  public String getSearchType() {
    return Indexer.STUDY_TYPE;
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    if (isOpenAccess()) return null;
    return () -> {
      List<String> ids = individualStudyService.findPublishedIds().stream()
        .filter(s -> subjectAclService.isAccessible("/individual-study", s)).collect(Collectors.toList());
      ids.addAll(harmonizationStudyService.findPublishedIds().stream()
        .filter(s -> subjectAclService.isAccessible("/harmonization-study", s)).collect(Collectors.toList()));
      return ids;
    };
  }

  @Override
  protected Taxonomy getTaxonomy() {
    return taxonomiesService.getStudyTaxonomy();
  }

  @Override
  protected List<AggregationMetaDataProvider> getAggregationMetaDataProviders() {
    return Arrays.asList(studyTaxonomyMetaDataProvider, studiesSetsAggregationMetaDataProvider);
  }

  @Override
  protected List<String> getMandatorySourceFields() {
    return Lists.newArrayList(
      "id",
      "className",
      "populations.id",
      "populations.dataCollectionEvents.id"
    );
  }

  @Override
  public void processHits(QueryResultDto.Builder builder, Searcher.DocumentResults results, QueryScope scope, CountStatsData counts)
    throws IOException {
    StudyResultDto.Builder resBuilder = StudyResultDto.newBuilder();
    StudyCountStatsBuilder studyCountStatsBuilder = counts == null ? null : StudyCountStatsBuilder.newBuilder(counts);
    Consumer<BaseStudy> addDto = getStudyConsumer(scope, resBuilder, studyCountStatsBuilder);
    List<BaseStudy> publishedStudies = getPublishedDocumentsFromHitsByClassName(results, BaseStudy.class);
    publishedStudies.forEach(addDto::accept);
    builder.setExtension(StudyResultDto.result, resBuilder.build());
  }

  private Consumer<BaseStudy> getStudyConsumer(QueryScope scope, StudyResultDto.Builder resBuilder,
                                               StudyCountStatsBuilder studyCountStatsBuilder) {

    return scope == QueryScope.DETAIL ? (study) -> {
      Mica.StudySummaryDto.Builder summaryBuilder = dtos.asSummaryDtoBuilder(study);
      if (mode == QueryMode.LIST) {
        summaryBuilder.clearPopulationSummaries();
      }
      if (studyCountStatsBuilder != null) {
        summaryBuilder.setCountStats(studyCountStatsBuilder.build(study))
          .build();
      }
      resBuilder.addSummaries(summaryBuilder.build());
    } : (study) -> resBuilder.addDigests(dtos.asDigestDtoBuilder(study).build());
  }

  @Nullable
  @Override
  protected Properties getAggregationsProperties(List<String> filter) {
    Properties properties = getAggregationsProperties(filter, taxonomiesService.getStudyTaxonomy());
    if (!properties.containsKey(JOIN_FIELD)) properties.put(JOIN_FIELD, "");
    return properties;
  }

  @Override
  public Map<String, Integer> getStudyCounts() {
    return getDocumentCounts(JOIN_FIELD);
  }

  public Map<String, Integer> getHarmonizationStudyCounts() {
    return getDocumentCountsFilteredByClassName(JOIN_FIELD, HarmonizationStudy.class.getSimpleName());
  }

  public Map<String, Integer> getIndividualStudyCounts() {
    return getDocumentCountsFilteredByClassName(JOIN_FIELD, Study.class.getSimpleName());
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(JOIN_FIELD);
  }
}
