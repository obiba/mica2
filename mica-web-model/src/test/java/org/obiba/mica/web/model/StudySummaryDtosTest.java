/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import org.junit.Test;
import org.obiba.mica.study.domain.Population;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class StudySummaryDtosTest {

  private StudySummaryDtos studySummaryDtos = new StudySummaryDtos();

  @Test
  public void can_extract_countries_from_populations() {

    List<Population> populations = populations(withCounties());

    Set<String> countries = studySummaryDtos.extractCountries(populations);

    assertThat(countries, containsInAnyOrder("FRA", "CAN"));
  }

  @Test
  public void when_null_country_in_population__return_empty_country_list() {

    List<Population> populations = populations(withNullAsCountry());

    Set<String> countries = studySummaryDtos.extractCountries(populations);

    assertThat(countries, hasSize(0));
  }

  private List<Population> populations(List<String> countriesIsos) {

    HashMap<String, Object> selectionCriteria = new HashMap<>();
    selectionCriteria.put("countriesIso", countriesIsos);

    HashMap<String, Object> model = new HashMap<>();
    model.put("selectionCriteria", selectionCriteria);

    List<Population> populations = new ArrayList<>();
    Population population = new Population();
    population.setModel(model);
    populations.add(population);
    return populations;
  }

  private List<String> withNullAsCountry() {
    return null;
  }

  private List<String> withCounties() {
    return asList("CAN", "FRA");
  }
}
