package org.obiba.mica.dataset.search.rest.harmonized;

import org.junit.Test;
import org.obiba.mica.web.model.Mica;

import com.google.common.collect.Lists;

import static org.assertj.core.api.Assertions.assertThat;

public class CombinedStatisticsTest {

  @SuppressWarnings("MagicNumber")
  @Test
  public void test_descriptive_statistics() {
    Mica.DatasetVariableAggregationDto z1 = newAggregation(20000, 20000, -1.622042f, 23.214470f, 11.016998f,
      220339.951481f, 181411.618186f, 3.011816f, 9.071034f);
    Mica.DatasetVariableAggregationDto z2 = newAggregation(10000, 10000, -9.871332f, 27.827914f, 8.995827f,
      89958.266541f, 247152.774475f, 4.971695f, 24.717749f);
    CombinedStatistics combined = new CombinedStatistics(Lists.newArrayList(z1, z2));

    assertThat(combined.getCount()).isEqualTo(30000);
    assertThat(combined.getMin()).isEqualTo(-9.871332f);
    assertThat(combined.getMax()).isEqualTo(27.827914f);
    assertThat(combined.getMean()).isEqualTo(10.343274f);
    assertThat(combined.getSum()).isEqualTo(310298.218023f);
//    assertThat(combined.getSumOfSquares()).isEqualTo(455798.605245f);
//    float gv = combined.getVariance();
//    assertThat(gv).isEqualTo(15.193793f);
//    assertThat(Double.valueOf(Math.pow(gv, 0.5)).floatValue()).isEqualTo(3.897922f);
  }

  private Mica.DatasetVariableAggregationDto newAggregation(int n, int total, float min, float max, float mean,
    float sum, float sum2, float sd, float var) {
    Mica.DatasetVariableAggregationDto.Builder z = Mica.DatasetVariableAggregationDto.newBuilder();
    z.setN(n);
    z.setTotal(total);
    z.setStatistics(Mica.StatisticsDto.newBuilder() //
      .setMin(min) //
      .setMax(max) //
      .setMean(mean) //
      .setSum(sum) //
      .setSumOfSquares(sum2) //
      .setStdDeviation(sd) //
      .setVariance(var) //
      .build());
    return z.build();
  }

}
