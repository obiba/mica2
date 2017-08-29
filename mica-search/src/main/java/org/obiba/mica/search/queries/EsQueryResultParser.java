/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
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

import javax.validation.constraints.NotNull;

import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.AggregationMetaDataResolver;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.mica.web.model.MicaSearch.RangeAggregationResultDto;

import static org.obiba.mica.web.model.MicaSearch.AggregationResultDto;
import static org.obiba.mica.web.model.MicaSearch.StatsAggregationResultDto;
import static org.obiba.mica.web.model.MicaSearch.TermsAggregationResultDto;

public class EsQueryResultParser {

  private final String locale;

  private final AggregationMetaDataResolver aggregationMetaDataResolver;

  private long totalCount;

  private EsQueryResultParser(AggregationMetaDataResolver metaDataResolver, String localeName) {
    aggregationMetaDataResolver = metaDataResolver;
    locale = localeName;
  }

  public static EsQueryResultParser newParser(AggregationMetaDataResolver aggregationTitleResolver, String locale) {
    return new EsQueryResultParser(aggregationTitleResolver, locale);
  }

  public List<AggregationResultDto> parseAggregations(@NotNull Aggregations aggregations) {
    List<AggregationResultDto> aggResults = new ArrayList();

    aggregations.forEach(aggregation -> {

      AggregationResultDto.Builder aggResultBuilder = AggregationResultDto.newBuilder();
      aggResultBuilder.setAggregation(aggregation.getName());
      String aggType = ((InternalAggregation) aggregation).type().name();

      switch(aggType) {
        case "stats":
          Stats stats = (Stats) aggregation;
          if(stats.getCount() > 0) {
            aggResultBuilder.setExtension(
              StatsAggregationResultDto.stats,
              StatsAggregationResultDto.newBuilder().setData(buildStatsDto(stats)).build());
          }
          break;
        case "terms":
          ((Terms) aggregation).getBuckets().forEach(bucket -> {
            TermsAggregationResultDto.Builder termsBuilder = TermsAggregationResultDto.newBuilder();

            if(bucket.getAggregations() != null) {
              termsBuilder.addAllAggs(parseAggregations(bucket.getAggregations()));
            }

            AggregationMetaDataProvider.MetaData metaData = aggregationMetaDataResolver
              .getMetaData(aggregation.getName(), bucket.getKeyAsString(), locale);
            if(metaData.hasTitle()) termsBuilder.setTitle(metaData.getTitle());
            if(metaData.hasDescription()) termsBuilder.setDescription(metaData.getDescription());
            if (metaData.hasClassName()) termsBuilder.setClassName(metaData.getClassName());
            if(metaData.hasStart()) termsBuilder.setStart(metaData.getStart());
            if(metaData.hasEnd()) termsBuilder.setEnd(metaData.getEnd());

            aggResultBuilder.addExtension(TermsAggregationResultDto.terms,
              termsBuilder.setKey(bucket.getKeyAsString()).setCount((int) bucket.getDocCount()).build());
          });
          break;

        case "range":
          ((Range) aggregation).getBuckets().forEach(bucket -> {
            AggregationMetaDataProvider.MetaData metaData = aggregationMetaDataResolver
              .getMetaData(aggregation.getName(), bucket.getKeyAsString(), locale);

            RangeAggregationResultDto.Builder rangeBuilder =
              RangeAggregationResultDto.newBuilder()
                .setDefault(-1)
                .setCount(bucket.getDocCount())
                .setKey(bucket.getKeyAsString())
                .setTitle(metaData.getTitle());

            if (metaData.hasDescription()) rangeBuilder.setDescription(metaData.getDescription());
            if (metaData.hasClassName()) rangeBuilder.setClassName(metaData.getClassName());

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
