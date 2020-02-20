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
import org.obiba.mica.search.aggregations.AggregationMetaDataResolver;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.spi.search.support.AggregationHelper;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.mica.web.model.MicaSearch.AggregationResultDto;
import org.obiba.mica.web.model.MicaSearch.RangeAggregationResultDto;
import org.obiba.mica.web.model.MicaSearch.StatsAggregationResultDto;
import org.obiba.mica.web.model.MicaSearch.TermsAggregationResultDto;

import javax.validation.constraints.NotNull;
import java.util.List;

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

  public List<AggregationResultDto> parseAggregations(@NotNull List<Searcher.DocumentAggregation> aggregations) {
    List<AggregationResultDto> aggResults = Lists.newArrayList();

    aggregations.forEach(aggregation -> {

      AggregationResultDto.Builder aggResultBuilder = AggregationResultDto.newBuilder();
      aggResultBuilder.setAggregation(aggregation.getName());
      String aggType = aggregation.getType();

      switch (aggType) {
        case AggregationHelper.AGG_STATS:
          Searcher.DocumentStatsAggregation stats = aggregation.asStats();
          if (stats.getCount() > 0) {
            aggResultBuilder.setExtension(
                StatsAggregationResultDto.stats,
                StatsAggregationResultDto.newBuilder().setData(buildStatsDto(stats)).build());
          }
          break;
        case AggregationHelper.AGG_TERMS:
        case AggregationHelper.AGG_STERMS:
          aggregation.asTerms().getBuckets().forEach(bucket -> {
            TermsAggregationResultDto.Builder termsBuilder = TermsAggregationResultDto.newBuilder();
            List<Searcher.DocumentAggregation> bucketAggregations = bucket.getAggregations();

            if (bucketAggregations != null && bucketAggregations.size() > 0) {
              termsBuilder.addAllAggs(parseAggregations(bucketAggregations));
            }

            AggregationMetaDataProvider.MetaData metaData = aggregationMetaDataResolver
                .getMetaData(aggregation.getName(), bucket.getKeyAsString(), locale);
            if (metaData.hasTitle()) termsBuilder.setTitle(metaData.getTitle());
            if (metaData.hasDescription()) termsBuilder.setDescription(metaData.getDescription());
            if (metaData.hasClassName()) termsBuilder.setClassName(metaData.getClassName());
            if (metaData.hasStart()) termsBuilder.setStart(metaData.getStart());
            if (metaData.hasEnd()) termsBuilder.setEnd(metaData.getEnd());
            if (metaData.hasSortField()) termsBuilder.setSortField(metaData.getSortField());

            aggResultBuilder.addExtension(TermsAggregationResultDto.terms,
                termsBuilder.setKey(bucket.getKeyAsString()).setCount((int) bucket.getDocCount()).build());
          });
          break;

        case AggregationHelper.AGG_RANGE:
          aggregation.asRange().getBuckets().forEach(bucket -> {
            AggregationMetaDataProvider.MetaData metaData = aggregationMetaDataResolver
                .getMetaData(aggregation.getName(), bucket.getKeyAsString(), locale);
            List<Searcher.DocumentAggregation> bucketAggregations = bucket.getAggregations();

            RangeAggregationResultDto.Builder rangeBuilder =
                RangeAggregationResultDto.newBuilder()
                    .setDefault(-1)
                    .setCount(bucket.getDocCount())
                    .setKey(bucket.getKeyAsString())
                    .setTitle(metaData.getTitle());

            if (bucketAggregations != null && bucketAggregations.size() > 0) {
              rangeBuilder.addAllAggs(parseAggregations(bucketAggregations));
            }

            if (metaData.hasDescription()) rangeBuilder.setDescription(metaData.getDescription());
            if (metaData.hasClassName()) rangeBuilder.setClassName(metaData.getClassName());

            Double from = bucket.getFrom();
            Double to = bucket.getTo();
            if (Double.NEGATIVE_INFINITY != from) {
              rangeBuilder.setFrom(from);
            }
            if (Double.POSITIVE_INFINITY != to) {
              rangeBuilder.setTo(to);
            }

            aggResultBuilder.addExtension(RangeAggregationResultDto.ranges, rangeBuilder.build());
          });

          break;

        case AggregationHelper.AGG_GLOBAL:
          totalCount = aggregation.asGlobal().getDocCount();
          // do not include in the list of aggregations
          return;

        default:
          throw new RuntimeException("Unsupported aggregation type " + aggType);
      }

      aggResults.add(aggResultBuilder.build());

    });

    return aggResults;
  }

  private MicaSearch.StatsAggregationResultDataDto buildStatsDto(Searcher.DocumentStatsAggregation stats) {
    MicaSearch.StatsAggregationResultDataDto.Builder builder = MicaSearch.StatsAggregationResultDataDto.newBuilder()
        .setCount(stats.getCount());

    if (!Double.isInfinite(stats.getMin())) {
      builder.setMin(stats.getMin());
    }

    if (!Double.isInfinite(stats.getMax())) {
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
