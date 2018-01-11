/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study;

import java.util.SortedSet;

import org.junit.Test;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;

import com.google.common.collect.Iterables;

import static org.obiba.mica.assertj.Assertions.assertThat;
import static org.obiba.mica.core.domain.LocalizedString.en;
import static org.obiba.mica.study.date.PersistableYearMonth.of;

@SuppressWarnings({ "MagicNumber", "OverlyLongMethod" })
public class TestDataCollectionEventSort {

  @Test
  public void test_all_date_fields() {
    Population population = createPopulation("Test Population", createEvent("A", 2010, 1, 2020, 12),
        createEvent("A", 1997, 1, 2000, 12), createEvent("A", 1997, 8, 2000, 12));
    population.setName(en("Test Population"));
    DataCollectionEvent event = Iterables.get(population.getDataCollectionEvents(), 0);
    assertThat(event.getStart()).isEqualTo(of(1997, 1));
  }

  @Test
  public void test_partial_date_fields() {
    Population population = createPopulation("Test Population", createEvent("A", 2010, null, 2020, null),
        createEvent("A", 1997, 1, 2000, null));
    DataCollectionEvent event = Iterables.get(population.getDataCollectionEvents(), 0);
    assertThat(event.getStart()).isEqualTo(of(1997, 1));
  }

  @Test
  public void test_no_end_year_date_fields() {
    Population population = createPopulation("Test Population", createEvent("A", 2010, null, null, null),
        createEvent("A", 1997, null, null, null));
    DataCollectionEvent event = Iterables.get(population.getDataCollectionEvents(), 0);
    assertThat(event.getStart()).isEqualTo(of(1997, 1));
  }

  @Test
  public void test_study_population_sort() {
    Study study = new Study();
    study.setId("01234567889");
    study.addPopulation(
        createPopulation("Population001", createEvent("A", 2010, 1, 2020, 12), createEvent("A", 2014, 1, 2035, 12)));
    study.addPopulation(
        createPopulation("Population001", createEvent("A", 1997, 8, 1998, 12), createEvent("A", 1996, 1, 2000, 12)));

    Population population = Iterables.get(study.getPopulations(), 0);
    DataCollectionEvent event = Iterables.get(population.getDataCollectionEvents(), 0);
    assertThat(event.getStart()).isEqualTo(of(1996, 1));
  }

  @Test
  public void test_study_population_sort_with_duplicate_event_dates() {
    Study study = new Study();
    study.setId("01234567889");
    study.addPopulation(
        createPopulation("Population001", createEvent("A", 2010, 1, 2020, 12), createEvent("B", 2014, 1, 2035, 12),
            createEvent("C", 2010, 1, 2020, 12)));

    Population population = Iterables.get(study.getPopulations(), 0);
    SortedSet<DataCollectionEvent> events = population.getDataCollectionEvents();
    assertThat(events.size()).isEqualTo(3);
    assertThat(Iterables.get(events, 0).getStart()).isEqualTo(of(2010, 1));
    assertThat(Iterables.get(events, 1).getStart()).isEqualTo(of(2010, 1));
  }

  @Test
  public void test_study_population_sort_with_duplicate_events() {
    Study study = new Study();
    study.setId("01234567889");
    study.addPopulation(createPopulation("Population001", createEvent("A", "A", 2010, 1, 2020, 12),
        createEvent("A", "A", 2014, 1, 2035, 12), createEvent("A", "A", 2010, 1, 2020, 12)));

    Population population = Iterables.get(study.getPopulations(), 0);
    SortedSet<DataCollectionEvent> events = population.getDataCollectionEvents();
    assertThat(events.size()).isEqualTo(2);
    assertThat(Iterables.get(events, 0).getStart()).isEqualTo(of(2010, 1));
    assertThat(Iterables.get(events, 1).getStart()).isEqualTo(of(2014, 1));
  }

  @Test
  public void test_study_populations_sort_with_duplicate_events() {
    Study study = new Study();
    study.setId("01234567889");
    study.addPopulation(createPopulation("Population001", createEvent("A", "A", 2010, 1, 2020, 12)));
    study.addPopulation(createPopulation("Population002", createEvent("A", "A", 2010, 1, 2020, 12)));
    study.addPopulation(createPopulation("Population003", createEvent("A", "A", 2010, 1, 2020, 12)));

    SortedSet<Population> populations = study.getPopulations();
    assertThat(populations.size()).isEqualTo(3);

    assertThat(Iterables.get(Iterables.get(populations, 0).getDataCollectionEvents(), 0).getStart())
        .isEqualTo(of(2010, 1));
    assertThat(Iterables.get(Iterables.get(populations, 1).getDataCollectionEvents(), 0).getStart())
        .isEqualTo(of(2010, 1));
    assertThat(Iterables.get(Iterables.get(populations, 2).getDataCollectionEvents(), 0).getStart())
        .isEqualTo(of(2010, 1));
  }

  @Test
  public void test_with_null_date_year_month_parts() {
    DataCollectionEvent event1 = createEvent("A", "A", null, null, null, null);
    DataCollectionEvent evetn2 = createEvent("A", "A", null, null, null, null);
    assertThat(event1.compareTo(evetn2)).isEqualTo(0);

    DataCollectionEvent event3 = createEvent("A", "A", null, null, null, null);
    DataCollectionEvent event4 = createEvent("B", "A", null, null, null, null);
    assertThat(event3.compareTo(event4)).isEqualTo(-1);

    DataCollectionEvent event5 = createEvent("A", "A", null, null, 2020, 12);
    DataCollectionEvent event6 = createEvent("B", "A", 2010, 1, 2020, 12);
    assertThat(event5.compareTo(event6)).isEqualTo(1);

    DataCollectionEvent event7 = createEvent("A", "A", 2010, 1, null, null);
    DataCollectionEvent event8 = createEvent("B", "A", 2010, 1, 2020, 12);
    assertThat(event7.compareTo(event8)).isEqualTo(1);

    DataCollectionEvent event9 = createEvent("A", "A", 2010, 1, 2020, 12);
    DataCollectionEvent event10 = createEvent("B", "A", null, null, 2020, 12);
    assertThat(event9.compareTo(event10)).isEqualTo(-1);

    DataCollectionEvent event11 = createEvent("A", "A", 2010, 1, 2020, 12);
    DataCollectionEvent event12 = createEvent("B", "A", 2010, 1, null, null);
    assertThat(event11.compareTo(event12)).isEqualTo(-1);

    DataCollectionEvent event13 = createEvent("A", "A", 2010, 1, 2020, 12);
    DataCollectionEvent event14 = createEvent("B", "A", null, null, null, null);
    assertThat(event13.compareTo(event14)).isEqualTo(-1);

    DataCollectionEvent event15 = createEvent("A", "A", 2010, 1, 2020, 12);
    DataCollectionEvent event16 = createEvent("A", "A", 2011, 1, 2020, 12);
    assertThat(event15.compareTo(event16)).isEqualTo(-1);

    DataCollectionEvent event17 = createEvent("A", "A", 2011, 1, 2020, 12);
    DataCollectionEvent event18 = createEvent("A", "A", 2010, 1, 2020, 12);
    assertThat(event17.compareTo(event18)).isEqualTo(1);
  }

  private Population createPopulation(String name, DataCollectionEvent... events) {
    Population population = new Population();
    population.setName(en(name));
    for(DataCollectionEvent event : events) {
      population.addDataCollectionEvent(event);
    }
    return population;
  }

  private DataCollectionEvent createEvent(String name, Integer startYear, Integer startMonth, Integer endYear,
      Integer endMonth) {
    return createEvent(null, name, startYear, startMonth, endYear, endMonth);
  }

  private DataCollectionEvent createEvent(String id, String name, Integer startYear, Integer startMonth,
      Integer endYear, Integer endMonth) {
    DataCollectionEvent event = new DataCollectionEvent();
    event.setId(id);
    event.setName(en(name));
    event.setDescription(en("Baseline data collection"));
    if(startYear != null) event.setStart(startYear, startMonth);
    if(endYear != null) event.setEnd(endYear, endMonth);
    return event;
  }
}
