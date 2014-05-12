package org.obiba.mica.domain;

import org.junit.Test;

import com.google.common.collect.Iterables;

import static org.obiba.mica.assertj.Assertions.assertThat;
import static org.obiba.mica.domain.LocalizedString.en;

public class TestDataCollectionEventSort {


  @Test
  public void test_all_date_fields() {
    Population population = createPopulation("Test Population", createEvent("A", 2010, 1, 2020, 12),
        createEvent("A", 1997, 1, 2000, 12), createEvent("A", 1997, 8, 2000, 12));
    population.setName(en("Test Population"));
    DataCollectionEvent event = Iterables.get(population.getDataCollectionEvents(), 0);
    assertThat(event.getStartYear()).isEqualTo(1997);
    assertThat(event.getStartMonth()).isEqualTo(1);
  }

  @Test
  public void test_partial_date_fields() {
    Population population = createPopulation("Test Population", createEvent("A", 2010, null, 2020, null),
        createEvent("A", 1997, 1, 2000, null));
    DataCollectionEvent event = Iterables.get(population.getDataCollectionEvents(), 0);
    assertThat(event.getStartYear()).isEqualTo(1997);
  }

  @Test
  public void test_no_end_year_date_fields() {
    Population population = createPopulation("Test Population", createEvent("A", 2010, null, null, null),
        createEvent("A", 1997, null, null, null));
    DataCollectionEvent event = Iterables.get(population.getDataCollectionEvents(), 0);
    assertThat(event.getStartYear()).isEqualTo(1997);
  }

  @Test
  public void test_study_population_sort() {
    Study study = new Study();
    study.setId("01234567889");
    study.addPopulation(createPopulation("Population001", createEvent("A", 2010, 1, 2020, 12),
        createEvent("A", 2014, 1, 2035, 12)));
    study.addPopulation(createPopulation("Population001", createEvent("A", 1997, 8, 1998, 12),
        createEvent("A", 1996, 1, 2000, 12)));

    Population population = Iterables.get(study.getPopulations(), 0);
    DataCollectionEvent event = Iterables.get(population.getDataCollectionEvents(), 0);
    assertThat(event.getStartYear()).isEqualTo(1996);
    assertThat(event.getStartMonth()).isEqualTo(1);
  }

  private Population createPopulation(String name, DataCollectionEvent... events) {
    Population population = new Population();
    population.setName(en(name));
    for (DataCollectionEvent event : events) {
      population.addDataCollectionEvent(event);
    }

    return population;
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
