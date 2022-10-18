/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.study.HarmonizationStudyStateRepository;
import org.obiba.mica.study.StudyStateRepository;
import org.obiba.mica.study.domain.StudyState;

import java.util.Locale;
import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StudyIdGeneratorServiceTest {

  @InjectMocks
  private StudyIdGeneratorService studyIdGeneratorService;

  @Mock
  private StudyStateRepository studyStateRepository;

  @Mock
  private HarmonizationStudyStateRepository harmonizationStudyStateRepository;

  @Test
  public void when_there_is_no_duplicate__return_escaped_string() {

    assertThat(generatedIdFor(acronym("studyId")), is("studyid"));
    assertThat(generatedIdFor(acronym("study_id")), is("study_id"));
    assertThat(generatedIdFor(acronym("study id")), is("study-id"));
    assertThat(generatedIdFor(acronym("study id", "id d'étude")), is("study-id-id-d_etude"));
    assertThat(generatedIdFor(acronym("study id", "identifiant d'étude")), is("study-id-identifiant-d_etude"));
  }

  @Test
  public void when_there_duplicate__return_id_with_incremented_number() {

    givenExistingAcronym("acronym");
    assertThat(generatedIdFor(acronym("acronym")), is("acronym-1"));
  }

  @Test
  public void when_there_is_incremented_duplicate__return_id_with_more_incremented_number() {

    givenExistingAcronym("acronym", "acronym-1", "acronym-2");
    assertThat(generatedIdFor(acronym("acronym")), is("acronym-3"));
  }

  @Test
  public void when_there_is_no_duplicate_for_incremented_acronym__return_given_acronym() {
    assertThat(generatedIdFor(acronym("acronym-5")), is("acronym-5"));
  }

  @Test
  public void when_there_is_number_at_the_end_of_the_acronym__return_given_acronym() {
    assertThat(generatedIdFor(acronym("acronym-2017")), is("acronym-2017"));
  }

  @Test
  public void when_there_is_number_at_the_end_of_the_acronym_and_duplicate__return_more_incremented_id() {
    givenExistingAcronym("acronym-2017");
    assertThat(generatedIdFor(acronym("acronym-2017")), is("acronym-2017-1"));
  }

  @Test
  public void when_existing_multilingual_id__return_incremented_multilingual_id() {

    givenExistingAcronym("acronym-2017-acronyme-2017");
    assertThat(generatedIdFor(acronym("acronym-2017", "acronyme 2017")), is("acronym-2017-acronyme-2017-1"));
  }

  private String generatedIdFor(LocalizedString id) {
    return studyIdGeneratorService.generateId(id);
  }

  private LocalizedString acronym(String englishId) {
    return new LocalizedString(Locale.ENGLISH, englishId);
  }

  private LocalizedString acronym(String englishId, String frenchId) {
    LocalizedString localizedString = acronym(englishId);
    localizedString.put(Locale.FRENCH, frenchId);
    return localizedString;
  }

  private void givenExistingAcronym(String... existingAcronyms) {
    for (String existingAcronym : existingAcronyms) {
      when(studyStateRepository.findById(existingAcronym)).thenReturn(Optional.of(new StudyState()));
    }
  }
}
