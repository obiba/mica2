/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.variable;

import javax.inject.Inject;

import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class CoverageByBucketFactory {

  @Inject
  private ApplicationContext applicationContext;

  public CoverageByBucket makeCoverageByBucket(MicaSearch.TaxonomiesCoverageDto coverage) {
    CoverageByBucket coverageByBucket = applicationContext.getBean(CoverageByBucket.class);
    coverageByBucket.initialize(coverage);
    return coverageByBucket;
  }

  public MicaSearch.BucketsCoverageDto asBucketsCoverageDto(MicaSearch.TaxonomiesCoverageDto coverage) {
    CoverageByBucket coverageByBucket = applicationContext.getBean(CoverageByBucket.class);
    coverageByBucket.initialize(coverage);
    MicaSearch.BucketsCoverageDto.Builder builder = MicaSearch.BucketsCoverageDto.newBuilder();
    coverageByBucket.getTaxonomyHeaders().forEach(taxonomyHeader -> {
      MicaSearch.BucketsCoverageDto.HeaderDto.Builder header = MicaSearch.BucketsCoverageDto.HeaderDto.newBuilder();
      header.setEntity(taxonomyHeader.taxonomy) //
        .setHits(taxonomyHeader.hits) //
        .setTermsCount(taxonomyHeader.termsCount);
      builder.addTaxonomyHeaders(header);
    });
    coverageByBucket.getVocabularyHeaders().forEach(vocabularyHeader -> {
      MicaSearch.BucketsCoverageDto.HeaderDto.Builder header = MicaSearch.BucketsCoverageDto.HeaderDto.newBuilder();
      header.setEntity(vocabularyHeader.vocabulary) //
        .setHits(vocabularyHeader.hits) //
        .setTermsCount(vocabularyHeader.termsCount);
      builder.addVocabularyHeaders(header);
    });
    coverageByBucket.getTermHeaders().forEach(termHeader -> {
      MicaSearch.BucketsCoverageDto.HeaderDto.Builder header = MicaSearch.BucketsCoverageDto.HeaderDto.newBuilder();
      header.setEntity(termHeader.term) //
        .setHits(termHeader.hits) //
        .setTermsCount(1);
      builder.addTermHeaders(header);
    });
    coverageByBucket.getBucketRows().forEach(bucketRow -> {
      MicaSearch.BucketsCoverageDto.RowDto.Builder row = MicaSearch.BucketsCoverageDto.RowDto.newBuilder();
      row.setField(bucketRow.field) //
        .setValue(bucketRow.value) //
        .setTitle(bucketRow.title) //
        .setDescription(bucketRow.description) //
        .setClassName(bucketRow.className) //
        .setStart(bucketRow.start) //
        .setEnd(bucketRow.end) //
        .addAllHits(bucketRow.hits) //
        .addAllCounts(bucketRow.counts);
      builder.addRows(row);
    });

    builder.setTotalCounts(totalCountsFromJoinQueryResult(coverage.getQueryResult()));
    return builder.build();
  }

  private MicaSearch.EntitiesTotalCountsDto.Builder totalCountsFromJoinQueryResult(MicaSearch.JoinQueryResultDto resultDto) {
    return MicaSearch.EntitiesTotalCountsDto.newBuilder()
      .setNetworkTotalCount(totalCountsFromQueryResult(resultDto.getNetworkResultDto()))
      .setDatasetTotalCount(totalCountsFromQueryResult(resultDto.getDatasetResultDto()))
      .setStudyTotalCount(totalCountsFromQueryResult(resultDto.getStudyResultDto()))
      .setVariableTotalCount(totalCountsFromQueryResult(resultDto.getVariableResultDto()));
  }

  private MicaSearch.EntityTotalCountDto.Builder totalCountsFromQueryResult(MicaSearch.QueryResultDto queryResult) {
    return MicaSearch.EntityTotalCountDto.newBuilder().setHits(queryResult.getTotalHits()).setTotal(queryResult.getTotalCount());
  }
}
