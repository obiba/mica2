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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

import org.obiba.mica.dataset.search.rest.variable.CoverageByBucket.BucketRow;
import org.obiba.mica.dataset.search.rest.variable.CoverageByBucket.TermHeader;
import org.obiba.mica.web.model.Mica.TaxonomyEntityDto;
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

  public MicaSearch.BucketsCoverageDto asBucketsCoverageDto(MicaSearch.TaxonomiesCoverageDto coverage, boolean withZeros) {
    CoverageByBucket coverageByBucket = applicationContext.getBean(CoverageByBucket.class);
    coverageByBucket.initialize(coverage);
    MicaSearch.BucketsCoverageDto.Builder builder = MicaSearch.BucketsCoverageDto.newBuilder();

    List<TermHeader> termHeaders = coverageByBucket.getTermHeaders();

    Map<String, Integer> taxonomyTermCounts = new HashMap<>();
    Map<String, Integer> vocabularyTermCounts = new HashMap<>();

    List<Integer> termIndices = new ArrayList<>();
    for (int i = 0; i < termHeaders.size(); i++) {
      TermHeader termHeader = termHeaders.get(i);

      String vocabularyName = termHeader.vocabulary.getName();
      String taxonomyName = termHeader.taxonomy.getName();

      if ((termHeader.hits > 0)) {
        if (!taxonomyTermCounts.containsKey(taxonomyName)) {
          taxonomyTermCounts.put(taxonomyName, 1);
        } else {
          taxonomyTermCounts.put(taxonomyName, taxonomyTermCounts.get(taxonomyName) + 1);
        }

        if (!vocabularyTermCounts.containsKey(vocabularyName)) {
          vocabularyTermCounts.put(vocabularyName, 1);
        } else {
          vocabularyTermCounts.put(vocabularyName, vocabularyTermCounts.get(vocabularyName) + 1);
        }

        termIndices.add(i);
      }
    }

    if (termIndices.size() == termHeaders.size() || withZeros) {
      termHeaders.forEach(termHeader -> addTermHeaderToCoverageBuilder(builder, termHeader));
      coverageByBucket.getBucketRows().forEach(bucketRow -> addBucketRowToCoverageBuilder(builder, bucketRow, bucketRow.hits));

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
    } else {
      for (Integer termIndex : termIndices) {
        addTermHeaderToCoverageBuilder(builder, termHeaders.get(termIndex));
      }

      coverageByBucket.getBucketRows().forEach(bucketRow -> {
        List<Integer> hits = new ArrayList<>();
        for (Integer termIndex : termIndices) {
          hits.add(bucketRow.hits.get(termIndex));
        }

        addBucketRowToCoverageBuilder(builder, bucketRow, hits);
      });

      coverageByBucket.getTaxonomyHeaders().forEach(taxonomyHeader -> {
        MicaSearch.BucketsCoverageDto.HeaderDto.Builder header = MicaSearch.BucketsCoverageDto.HeaderDto.newBuilder();
        header.setEntity(taxonomyHeader.taxonomy) //
          .setHits(taxonomyHeader.hits);

        if (taxonomyHeader.taxonomy != null) {
          header.setTermsCount(taxonomyTermCounts.getOrDefault(taxonomyHeader.taxonomy.getName(), 0));
        }

        builder.addTaxonomyHeaders(header);
      });
      coverageByBucket.getVocabularyHeaders().forEach(vocabularyHeader -> {
        MicaSearch.BucketsCoverageDto.HeaderDto.Builder header = MicaSearch.BucketsCoverageDto.HeaderDto.newBuilder();
        header.setEntity(vocabularyHeader.vocabulary) //
          .setHits(vocabularyHeader.hits);

        if (vocabularyHeader.vocabulary != null) {
          header.setTermsCount(vocabularyTermCounts.getOrDefault(vocabularyHeader.vocabulary.getName(), 0));
        }

        builder.addVocabularyHeaders(header);
      });
    }

    builder.setTotalCounts(totalCountsFromJoinQueryResult(coverage.getQueryResult()));
    return builder.build();
  }

  private void addBucketRowToCoverageBuilder(MicaSearch.BucketsCoverageDto.Builder builder, BucketRow bucketRow, List<Integer> hits) {
    MicaSearch.BucketsCoverageDto.RowDto.Builder row = MicaSearch.BucketsCoverageDto.RowDto.newBuilder();
    row.setField(bucketRow.field) //
      .setValue(bucketRow.value) //
      .setTitle(bucketRow.title) //
      .setDescription(bucketRow.description) //
      .setClassName(bucketRow.className) //
      .setStart(bucketRow.start) //
      .setEnd(bucketRow.end) //
      .setSortField(bucketRow.sortField)
      .addAllHits(hits) //
      .addAllCounts(bucketRow.counts);
    builder.addRows(row);
  }

  private void addTermHeaderToCoverageBuilder(MicaSearch.BucketsCoverageDto.Builder builder, TermHeader termHeader) {
    MicaSearch.BucketsCoverageDto.HeaderDto.Builder header = MicaSearch.BucketsCoverageDto.HeaderDto.newBuilder();
    header.setEntity(termHeader.term) //
      .setHits(termHeader.hits) //
      .setTermsCount(1);
    builder.addTermHeaders(header);
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
