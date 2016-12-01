/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.AggregationMetaDataResolver;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.mica.web.model.MicaSearch.RangeAggregationResultDto;

import com.google.common.collect.Lists;

import static org.obiba.mica.web.model.MicaSearch.AggregationResultDto;
import static org.obiba.mica.web.model.MicaSearch.StatsAggregationResultDto;
import static org.obiba.mica.web.model.MicaSearch.TermsAggregationResultDto;

public class EsQueryResultParser {

  private final String locale;

  private final AggregationMetaDataResolver aggregationTitleResolver;

  private long totalCount;

  private EsQueryResultParser(AggregationMetaDataResolver titleResolver, String localeName) {
    aggregationTitleResolver = titleResolver;
    locale = localeName;
  }

  public static EsQueryResultParser newParser(AggregationMetaDataResolver aggregationTitleResolver, String locale) {
    return new EsQueryResultParser(aggregationTitleResolver, locale);
  }

  public List<AggregationResultDto> parseAggregations(@Nullable Aggregations defaults, @NotNull Aggregations queried) {
    if (defaults == null) return parseAggregations(queried);

    List<Aggregation> defaultAggs = defaults.asList();
    List<Aggregation> queriedAggs = queried.asList();
    List<AggregationResultDto> aggResults = Lists.newArrayList();

    IntStream.range(0, defaultAggs.size()).forEach(i -> {
      Aggregation defaultAgg = defaultAggs.get(i);
      Aggregation queriedAgg = queriedAggs.get(i);

      AggregationResultDto.Builder aggResultBuilder = AggregationResultDto.newBuilder();
      aggResultBuilder.setAggregation(defaultAgg.getName());
      String aggType = defaultAgg.getName();

      switch(aggType) {
        case "stats":
          Stats defaultStats = (Stats) defaultAgg;
          Stats queriedStats = (Stats) queriedAgg;
          if(defaultStats.getCount() > 0) {
            MicaSearch.StatsAggregationResultDataDto defaultStatsDto = buildStatsDto(defaultStats);
            MicaSearch.StatsAggregationResultDataDto dataStatsDto = buildStatsDto(queriedStats);
            aggResultBuilder.setExtension(StatsAggregationResultDto.stats, //
              StatsAggregationResultDto.newBuilder() //
                .setDefault(defaultStatsDto) //
                .setData(dataStatsDto).build()); //
          }
          break;
        case "terms":
          List<Terms.Bucket> defaultBuckets = ((Terms) defaultAgg).getBuckets().stream().collect(Collectors.toList());
          List<Terms.Bucket> queriedBuckets = ((Terms) queriedAgg).getBuckets().stream().collect(Collectors.toList());

          Map<String, Terms.Bucket> queriedBucketsMap = queriedBuckets.stream()
            .collect(Collectors.toMap(MultiBucketsAggregation.Bucket::getKeyAsString, r -> r));
          int queriedBucketsSize = queriedBuckets.size();

          IntStream.range(0, defaultBuckets.size()).forEach(j -> {
            // It appears that 'min_document_count' does not apply to sub-aggregations,
            // hence the disparity in bucket sizes
            Terms.Bucket defaultBucket = defaultBuckets.get(j);
            Optional<Terms.Bucket> queriedBucket = queriedBucketsSize == defaultBuckets.size()
              ? Optional.ofNullable(queriedBuckets.get(j))
              : queriedBucketsSize > 0 ? Optional.ofNullable(queriedBucketsMap.get(defaultBucket.getKey())) : Optional.empty();

            int queriedBucketCount = 0;
            Optional<Aggregations> queriedAggregations = Optional.empty();

            if(queriedBucket.isPresent()) {
              Terms.Bucket bucket = queriedBucket.get();
              queriedBucketCount = (int) bucket.getDocCount();
              queriedAggregations = Optional.ofNullable(bucket.getAggregations());
            }

            TermsAggregationResultDto.Builder termsBuilder = TermsAggregationResultDto.newBuilder();

            if(defaultBucket.getAggregations() != null && queriedAggregations.isPresent()) {
              termsBuilder.addAllAggs(parseAggregations(defaultBucket.getAggregations(), queriedAggregations.get()));
            }

            String key = defaultBucket.getKeyAsString();

            AggregationMetaDataProvider.MetaData metaData = aggregationTitleResolver
              .getTitle(defaultAgg.getName(), key, locale);
            if(metaData.hasTitle()) termsBuilder.setTitle(metaData.getTitle());
            if(metaData.hasDescription()) termsBuilder.setDescription(metaData.getDescription());
            if(metaData.hasStart()) termsBuilder.setStart(metaData.getStart());
            if(metaData.hasEnd()) termsBuilder.setEnd(metaData.getEnd());

            aggResultBuilder.addExtension(TermsAggregationResultDto.terms, //
              termsBuilder.setKey(key) //
                .setDefault((int) defaultBucket.getDocCount()) //
                .setCount(queriedBucketCount).build()); //
          });

          break;

        case "range":
          List<? extends Range.Bucket> defaultRangeBuckets = ((Range) defaultAgg).getBuckets();
          List<? extends Range.Bucket> queriedRangeBuckets = ((Range) queriedAgg).getBuckets();
          IntStream.range(0, defaultRangeBuckets.size()).forEach(j -> {
            Range.Bucket defaultBucket = defaultRangeBuckets.get(j);
            Range.Bucket queriedBucket = queriedRangeBuckets.get(j);

            AggregationMetaDataProvider.MetaData metaData = aggregationTitleResolver
              .getTitle(queriedAgg.getName(), defaultBucket.getKeyAsString(), locale);
            RangeAggregationResultDto.Builder rangeBuilder = RangeAggregationResultDto.newBuilder();
            rangeBuilder.setDefault(defaultBucket.getDocCount());
            rangeBuilder.setCount(queriedBucket.getDocCount());
            rangeBuilder.setKey(defaultBucket.getKeyAsString());
            rangeBuilder.setTitle(metaData.getTitle());

            if (metaData.hasDescription()) rangeBuilder.setDescription(metaData.getDescription());

            Double from = (Double)queriedBucket.getFrom();
            Double to = (Double)queriedBucket.getTo();
            if (Double.NEGATIVE_INFINITY != from) {
              rangeBuilder.setFrom(from);
            }
            if (Double.POSITIVE_INFINITY != to) {
              rangeBuilder.setTo(to);
            }

            aggResultBuilder.addExtension(RangeAggregationResultDto.ranges, rangeBuilder.build());
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

  public List<AggregationResultDto> parseAggregations(@NotNull Aggregations aggregations) {
    List<AggregationResultDto> aggResults = new ArrayList();

    aggregations.forEach(aggregation -> {

      AggregationResultDto.Builder aggResultBuilder = AggregationResultDto.newBuilder();
      aggResultBuilder.setAggregation(aggregation.getName());
      String aggType = aggregation.getName();

      switch(aggType) {
        case "stats":
          Stats stats = (Stats) aggregation;
          if(stats.getCount() > 0) {
            aggResultBuilder.setExtension(StatsAggregationResultDto.stats, //
              StatsAggregationResultDto.newBuilder() //
                .setDefault(MicaSearch.StatsAggregationResultDataDto.newBuilder() //
                  .setCount(-1) //
                  .setMin(-1) //
                  .setMax(-1) //
                  .setAvg(-1) //
                  .setSum(-1).build()) //
                .setData(MicaSearch.StatsAggregationResultDataDto.newBuilder() //
                  .setCount(stats.getCount()) //
                  .setMin(stats.getMin()) //
                  .setMax(stats.getMax()) //
                  .setAvg(stats.getAvg()) //
                  .setSum(stats.getSum()).build()) //
                .build()); //
          }
          break;
        case "terms":
          ((Terms) aggregation).getBuckets().forEach(bucket -> {
            TermsAggregationResultDto.Builder termsBuilder = TermsAggregationResultDto.newBuilder();

            if(bucket.getAggregations() != null) {
              termsBuilder.addAllAggs(parseAggregations(bucket.getAggregations()));
            }

            AggregationMetaDataProvider.MetaData metaData = aggregationTitleResolver
              .getTitle(aggregation.getName(), bucket.getKeyAsString(), locale);
            if(metaData.hasTitle()) termsBuilder.setTitle(metaData.getTitle());
            if(metaData.hasDescription()) termsBuilder.setDescription(metaData.getDescription());
            if(metaData.hasStart()) termsBuilder.setStart(metaData.getStart());
            if(metaData.hasEnd()) termsBuilder.setEnd(metaData.getEnd());

            aggResultBuilder.addExtension(TermsAggregationResultDto.terms,
              termsBuilder.setKey(bucket.getKeyAsString()).setDefault(-1).setCount((int) bucket.getDocCount()).build());
          });
          break;

        case "range":
          ((Range) aggregation).getBuckets().stream().forEach(bucket -> {
            AggregationMetaDataProvider.MetaData metaData = aggregationTitleResolver
              .getTitle(aggregation.getName(), bucket.getKeyAsString(), locale);

            RangeAggregationResultDto.Builder rangeBuilder =
              RangeAggregationResultDto.newBuilder()
                .setDefault(-1)
                .setCount(bucket.getDocCount())
                .setKey(bucket.getKeyAsString())
                .setTitle(metaData.getTitle());

            if (metaData.hasDescription()) rangeBuilder.setDescription(metaData.getDescription());

            Double from = (Double)bucket.getFrom();
            Double to = (Double)bucket.getTo();
            if (Double.NEGATIVE_INFINITY != from ) {
              rangeBuilder.setFrom(from);
            }
            if (Double.POSITIVE_INFINITY != to) {
              rangeBuilder.setTo(to);
            }

            aggResultBuilder.addExtension(RangeAggregationResultDto.ranges, rangeBuilder.build());
          });

          break;

        case "global":
          totalCount = ((Global) aggregation).getDocCount();
          // do not include in the list of aggregations
          return;

        default:
          throw new RuntimeException("Unsupported aggregation type " + aggType);
      }

      aggResults.add(aggResultBuilder.build());

    });

    return aggResults;
  }

  private MicaSearch.StatsAggregationResultDataDto buildStatsDto(Stats stats) {
    MicaSearch.StatsAggregationResultDataDto.Builder builder = MicaSearch.StatsAggregationResultDataDto.newBuilder()
      .setCount(stats.getCount());

    if(!Double.isInfinite(stats.getMin())) {
      builder.setMin(stats.getMin());
    }

    if(!Double.isInfinite(stats.getMax())) {
      builder.setMax(stats.getMax());
    }

    if (!Double.isNaN(stats.getAvg())) {
      builder.setAvg(stats.getAvg());
    }

    builder.setSum(stats.getSum());

    return builder.build();
  }

  public long getTotalCount() {
    return totalCount;
  }

}
