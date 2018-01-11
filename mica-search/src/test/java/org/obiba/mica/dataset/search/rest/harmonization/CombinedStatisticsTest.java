/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.harmonization;

import org.junit.Test;
import org.obiba.mica.web.model.Mica;

import com.google.common.collect.Lists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class CombinedStatisticsTest {

  /**
   * Use following R code to generate some data.
   * <pre>
   * <code>
   * z1 = rnorm(20000, mean=11, sd=3)
   * hist(z1,freq=FALSE,nclass=100)
   * z2 = rnorm(10000, mean=9, sd=5)
   * hist(z2,freq=FALSE,nclass=100)
   * z3 = append(z1,z2)
   * length(z3)
   * hist(z3,freq=FALSE,nclass=100)
   *
   * print_summary = function(z) {
   *   s = c(n=length(z), min=min(z), max=max(z), mean=mean(z), sd=sd(z), sum=sum(z), sum2=sum((z - mean(z))^2), var=var(z))
   *   print(s)
   * }
   *
   * print_summary(z1)
   * print_summary(z2)
   * print_summary(z3)
   * </code>
   * </pre>
   */
  @SuppressWarnings("MagicNumber")
  @Test
  public void test_descriptive_statistics() {
    Mica.DatasetVariableAggregationDto z1 = newAggregation(20000, 20000, -1.622042d, 23.214470d, 11.016998d,
      220339.951481d, 181411.618186d, 3.011816d, 9.071034d);
    Mica.DatasetVariableAggregationDto z2 = newAggregation(10000, 10000, -9.871332d, 27.827914d, 8.995827d,
      89958.266541d, 247152.774475d, 4.971695d, 24.717749d);
    CombinedStatistics combined = new CombinedStatistics(Lists.newArrayList(z1, z2));

    assertThat(combined.getCount()).isEqualTo(30000);
    assertThat(combined.getMin()).isCloseTo(-9.871332d, within(0.00001d));
    assertThat(combined.getMax()).isCloseTo(27.827914d, within(0.00001d));
    assertThat(combined.getMean()).isCloseTo(10.343274d, within(0.00001d));
    assertThat(combined.getSum()).isCloseTo(310298.218023d, within(0.00001d));
//    assertThat(combined.getSumOfSquares()).isEqualTo(455798.605245f);
//    float gv = combined.getVariance();
//    assertThat(gv).isEqualTo(15.193793f);
//    assertThat(Double.valueOf(Math.pow(gv, 0.5)).floatValue()).isEqualTo(3.897922f);
  }

  private Mica.DatasetVariableAggregationDto newAggregation(int n, int total, double min, double max, double mean,
                                                            double sum, double sum2, double sd, double var) {
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
