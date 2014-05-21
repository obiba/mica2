package org.obiba.mica.domain;

import java.util.SortedSet;

import org.junit.Test;

import com.google.common.base.Strings;
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

  @Test
  public void test_study_population_sort_with_duplicate_event_dates() {
    Study study = new Study();
    study.setId("01234567889");
    study.addPopulation(createPopulation("Population001", createEvent("A", 2010, 1, 2020, 12),
        createEvent("B", 2014, 1, 2035, 12), createEvent("C", 2010, 1, 2020, 12)));

    Population population = Iterables.get(study.getPopulations(), 0);
    SortedSet<DataCollectionEvent> events = population.getDataCollectionEvents();
    assertThat(events.size()).isEqualTo(3);
    assertThat(Iterables.get(events, 0).getStartYear()).isEqualTo(2010);
    assertThat(Iterables.get(events, 1).getStartYear()).isEqualTo(2010);
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
    assertThat(Iterables.get(events, 0).getStartYear()).isEqualTo(2010);
    assertThat(Iterables.get(events, 1).getStartYear()).isEqualTo(2014);
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
    assertThat(Iterables.get((Iterables.get(populations, 0).getDataCollectionEvents()), 0).getStartYear())
        .isEqualTo(2010);
    assertThat(Iterables.get((Iterables.get(populations, 1).getDataCollectionEvents()), 0).getStartYear())
        .isEqualTo(2010);
    assertThat(Iterables.get((Iterables.get(populations, 2).getDataCollectionEvents()), 0).getStartYear())
        .isEqualTo(2010);
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
    return createEvent(null, name, startYear, startMonth, endYear, endMonth);
  }

  private DataCollectionEvent createEvent(String id, String name, Integer startYear, Integer startMonth, Integer endYear,
      Integer endMonth) {
    DataCollectionEvent event = new DataCollectionEvent();
    if (!Strings.isNullOrEmpty(id)) event.setId(id);
    event.setName(en(name));
    event.setDescription(en("Baseline data collection"));
    event.setStartYear(startYear);
    if (startMonth != null) event.setStartMonth(startMonth);
    if (endYear != null) event.setEndYear(endYear);
    if (endMonth != null) event.setEndMonth(endMonth);
    return event;
  }
}
