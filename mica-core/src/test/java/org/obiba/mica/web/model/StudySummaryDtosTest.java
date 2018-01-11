/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.obiba.mica.study.domain.Population;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
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

  @Test
  public void when_targetNumber_exists_in_model__add_it_in_dtoBuilder() {

    // Given
    Mica.StudySummaryDto.Builder studyBuilder = Mica.StudySummaryDto.newBuilder();
    Map<String, Object> studyModel = ImmutableMap.<String, Object>builder()
      .put("numberOfParticipants", ImmutableMap.<String, Object>builder()
        .put("participant", ImmutableMap.<String, Object>builder()
          .put("number", 20)
          .put("noLimit", false)
          .build()).build()).build();

    // Execute
    studySummaryDtos.addTargetNumberInBuilderIfPossible(studyModel, studyBuilder);

    // Verify
    assertThat(studyBuilder.getTargetNumber().getNumber(), is(20));
    assertThat(studyBuilder.getTargetNumber().getNoLimit(), is(false));
  }

  @Test
  public void when_targetNumber_not_exists_in_model__do_nothing() {

    // Given
    Mica.StudySummaryDto.Builder studyBuilder = Mica.StudySummaryDto.newBuilder();
    Map<String, Object> studyModel = ImmutableMap.<String, Object>builder().build();

    // Execute
    studySummaryDtos.addTargetNumberInBuilderIfPossible(studyModel, studyBuilder);

    // Verify
    assertThat(studyBuilder.hasTargetNumber(), is(false));
  }

  @Test
  public void when_targetNumber_has_invalid_data__do_nothing() {

    // Given
    Mica.StudySummaryDto.Builder studyBuilder = Mica.StudySummaryDto.newBuilder();
    Map<String, Object> studyModel = ImmutableMap.<String, Object>builder()
      .put("numberOfParticipants", ImmutableMap.<String, Object>builder()
        .put("participant", ImmutableMap.<String, Object>builder()
          .put("number", "20")
          .put("noLimit", false)
          .build()).build()).build();

    // Execute
    studySummaryDtos.addTargetNumberInBuilderIfPossible(studyModel, studyBuilder);

    // Verify
    assertThat(studyBuilder.hasTargetNumber(), is(false));
  }

  @Test
  public void when_design_exists_in_model__add_it_in_dtoBuilder() {

    // Given
    Mica.StudySummaryDto.Builder studyBuilder = Mica.StudySummaryDto.newBuilder();
    Map<String, Object> studyModel = ImmutableMap.<String, Object>builder()
      .put("methods", ImmutableMap.<String, Object>builder()
        .put("design", "cohort")
        .build()).build();

    // Execute
    studySummaryDtos.addDesignInBuilderIfPossible(studyModel, studyBuilder);

    // Verify
    assertThat(studyBuilder.getDesign(), is("cohort"));
  }

  @Test
  public void when_design_not_exists_in_model__ignore_it() {

    // Given
    Mica.StudySummaryDto.Builder studyBuilder = Mica.StudySummaryDto.newBuilder();
    Map<String, Object> studyModel = ImmutableMap.<String, Object>builder().build();

    // Execute
    studySummaryDtos.addDesignInBuilderIfPossible(studyModel, studyBuilder);

    // Verify
    assertThat(studyBuilder.hasDesign(), is(false));
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
