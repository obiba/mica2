package org.obiba.mica.domain;

import org.junit.Test;

import com.google.common.collect.Iterables;

import static org.obiba.mica.assertj.Assertions.assertThat;
import static org.obiba.mica.domain.LocalizedString.en;

public class TestDataCollectionEventSort {


  @Test
  public void test_all_date_fields() {
    Population population = new Population();
    population.setName(en("Test Population"));
    population.addDataCollectionEvent(createEvent("A", 2010, 1, 2020, 12));
    population.addDataCollectionEvent(createEvent("A", 1997, 1, 2000, 12));
    population.addDataCollectionEvent(createEvent("A", 1997, 8, 2000, 12));
    DataCollectionEvent event = Iterables.get(population.getDataCollectionEvents(), 0);
    assertThat(event.getStartYear()).isEqualTo(1997);
    assertThat(event.getStartMonth()).isEqualTo(1);
  }

  @Test
  public void test_partial_date_fields() {
    Population population = new Population();
    population.setName(en("Test Population"));
    population.addDataCollectionEvent(createEvent("A", 2010, null, 2020, null));
    population.addDataCollectionEvent(createEvent("A", 1997, 1, 2000, null));
    DataCollectionEvent event = Iterables.get(population.getDataCollectionEvents(), 0);
    assertThat(event.getStartYear()).isEqualTo(1997);
  }

  @Test
  public void test_no_end_year_date_fields() {
    Population population = new Population();
    population.setName(en("Test Population"));
    population.addDataCollectionEvent(createEvent("A", 2010, null, null, null));
    population.addDataCollectionEvent(createEvent("A", 1997, null, null, null));
    DataCollectionEvent event = Iterables.get(population.getDataCollectionEvents(), 0);
    assertThat(event.getStartYear()).isEqualTo(1997);
  }

  private DataCollectionEvent createEvent(String name, Integer startYear, Integer startMonth, Integer endYear,
      Integer endMonth) {
    DataCollectionEvent event = new DataCollectionEvent();
    event.setName(en(name));
    event.setDescription(en("Baseline data collection"));
    event.setStartYear(startYear);
    if (startMonth != null) event.setStartMonth(startMonth);
    if (endYear != null) event.setEndYear(endYear);
    if (endMonth != null) event.setEndMonth(endMonth);
    return event;
  }
}
