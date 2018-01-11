/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.assertj;

import org.assertj.core.api.AbstractAssert;
import org.obiba.mica.study.domain.Study;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class StudyAssert extends AbstractAssert<StudyAssert, Study> {

  private final ObjectMapper objectMapper;

  protected StudyAssert(Study actual) {
    super(actual, StudyAssert.class);
    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
  }

  public StudyAssert areFieldsEqualToEachOther(Study expected) {
    JsonNode actualJson = objectMapper.valueToTree(actual);
    JsonNode expectedJson = objectMapper.valueToTree(expected);
    assertThat(actualJson).isEqualTo(expectedJson);
    return myself;
  }
}
