/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.validation.constraints.NotNull;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.obiba.mica.search.AggregationTitleResolver;
import org.obiba.mica.web.model.MicaSearch;

import static org.obiba.mica.web.model.MicaSearch.AggregationResultDto;
import static org.obiba.mica.web.model.MicaSearch.StatsAggregationResultDto;
import static org.obiba.mica.web.model.MicaSearch.TermsAggregationResultDto;

public class EsQueryResultParser {

  private final String locale;

  private final AggregationTitleResolver aggregationTitleResolver;

  private long totalCount;

  private EsQueryResultParser(AggregationTitleResolver titleResolver, String localeName) {
    aggregationTitleResolver = titleResolver;
    locale = localeName;
  }

  public static EsQueryResultParser newParser(AggregationTitleResolver aggregationTitleResolver, String locale) {
    return new EsQueryResultParser(aggregationTitleResolver, locale);
  }

  public List<AggregationResultDto> parseAggregations(@NotNull Aggregations defaults, @NotNull Aggregations queried) {
    List<AggregationResultDto> aggResults = new ArrayList();
    List<Aggregation> defaultAggs = defaults.asList();
    List<Aggregation> queriedAggs = queried.asList();

    IntStream.range(0, defaultAggs.size()).forEach(i -> {
      Aggregation defaultAgg = defaultAggs.get(i);
      Aggregation queriedAgg = queriedAggs.get(i);

      AggregationResultDto.Builder aggResultBuilder = MicaSearch.AggregationResultDto.newBuilder();
      aggResultBuilder.setAggregation(defaultAgg.getName());
      String aggType = ((InternalAggregation) defaultAgg).type().name();

      switch(aggType) {
        case "stats":
          Stats defaultStats = (Stats) defaultAgg;
          Stats queriedStats = (Stats) queriedAgg;
          if(defaultStats.getCount() > 0) {
            aggResultBuilder.setExtension(StatsAggregationResultDto.stats, //
              StatsAggregationResultDto.newBuilder() //
                .setDefault(MicaSearch.StatsAggregationResultDataDto.newBuilder() //
                  .setCount(defaultStats.getCount()) //
                  .setMin(defaultStats.getMin()) //
                  .setMax(defaultStats.getMax()) //
                  .setAvg(defaultStats.getAvg()) //
                  .setSum(defaultStats.getSum()).build()) //
                .setData(MicaSearch.StatsAggregationResultDataDto.newBuilder() //
                  .setCount(queriedStats.getCount()) //
                  .setMin(queriedStats.getMin()) //
                  .setMax(queriedStats.getMax()) //
                  .setAvg(queriedStats.getAvg()) //
                  .setSum(queriedStats.getSum()).build()) //
                .build()); //
          }
          break;
        case "terms":
          List<Terms.Bucket> defaultBuckets = ((Terms) defaultAgg).getBuckets().stream().collect(Collectors.toList());
          List<Terms.Bucket> queriedBuckets = ((Terms) queriedAgg).getBuckets().stream().collect(Collectors.toList());

          IntStream.range(0, defaultBuckets.size()).forEach(j -> {
            // It appears that 'min_document_count' does not apply to sub-aggregations,
            // hence the disparity in bucket sizes
            Terms.Bucket queriedBucket = queriedBuckets.size() > 0 ? queriedBuckets.get(j) : null;

            int queriedBucketCount = queriedBucket != null ? (int)queriedBucket.getDocCount() : 0;
            Terms.Bucket defaultBucket = defaultBuckets.get(j);
            TermsAggregationResultDto.Builder termsBuilder = TermsAggregationResultDto.newBuilder();

            if(defaultBucket.getAggregations() != null && queriedBucket != null) {
              termsBuilder
                  .addAllAggs(parseAggregations(defaultBucket.getAggregations(), queriedBucket.getAggregations()));
            }

            String key = defaultBucket.getKey();
            termsBuilder.setTitle(aggregationTitleResolver.getTitle(defaultAgg.getName(), key, locale));

            aggResultBuilder.addExtension(TermsAggregationResultDto.terms, //
                termsBuilder.setKey(key) //
                    .setDefault((int) defaultBucket.getDocCount()) //
                    .setCount(queriedBucketCount).build()); //
          });

          break;
        case "global":
          totalCount = ((Global) defaultAgg).getDocCount();
          // do not include in the list of aggregations
          return;

        default:
          throw new RuntimeException("Unsupported aggregation type " + aggType);
      }

      aggResults.add(aggResultBuilder.build());
    });

    return aggResults;
  }

  public long getTotalCount() {
    return totalCount;
  }

}
