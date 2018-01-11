/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.mica.config.JsonConfiguration;
import org.obiba.mica.study.date.PersistableYearMonth;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = { JsonConfiguration.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PersistableDateTest {

  @Test
  public void test_year_creation_format() {
    assertThat(PersistableYearMonth.of(2009).getYearMonth()).isEqualTo("2009");
  }

  @Test
  public void test_year_month_creation_format() {
    assertThat(PersistableYearMonth.of(1989, 1).getYearMonth()).isEqualTo("1989-01");
    assertThat(PersistableYearMonth.of(1989, 11).getYearMonth()).isEqualTo("1989-11");
  }

  @Test
  public void test_year_month_invalid_year() {
    assertThat(test_invalid_year_month(19, 1)).isTrue();
    assertThat(test_invalid_year_month(1800, 1)).isTrue();
  }

  @Test
  public void test_year_invalid_year() {
    assertThat(test_invalid_year(12)).isTrue();
    assertThat(test_invalid_year(10990)).isTrue();
  }

  @Test
  public void test_year_month_invalid_month() {
    assertThat(test_invalid_year_month(1989, 0)).isTrue();
    assertThat(test_invalid_year_month(1989, 13)).isTrue();
  }

  @Test
  public void test_get_year_month() {
    PersistableYearMonth.YearMonthData data = PersistableYearMonth.of(1989, 1).getYearMonthData();
    assertThat(data.getYear()).isEqualTo(1989);
    assertThat(data.getMonth()).isEqualTo(1);
  }

  @Test
  public void test_get_year_data() {
    PersistableYearMonth.YearMonthData data = PersistableYearMonth.of(2010).getYearMonthData();
    assertThat(data.getYear()).isEqualTo(2010);
    assertThat(data.getMonth()).isEqualTo(0);
  }

  @Test
  public void test_get_year_month_data() {
    PersistableYearMonth.YearMonthData data = PersistableYearMonth.of(1989, 1).getYearMonthData();
    assertThat(data.getYear()).isEqualTo(1989);
    assertThat(data.getMonth()).isEqualTo(1);
  }

  private boolean test_invalid_year_month(int year, int month) {
    try {
      PersistableYearMonth.of(year, month);
    } catch (IllegalArgumentException e) {
      return true;
    }

    return false;
  }

  private boolean test_invalid_year(int year) {
    try {
      PersistableYearMonth.of(year);
    } catch (IllegalArgumentException e) {
      return true;
    }
    return false;
  }
}
