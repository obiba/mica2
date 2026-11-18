/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The study import compares the forms of both Mica instances whatever their dialect (#4612).
 */
public class StudiesImportDataShapeTest {

  @Test
  public void test_data_shape_ignores_the_form_dialect() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode asf = mapper.readTree("{\"type\":\"object\",\"properties\":{"
      + "\"status\":{\"type\":\"string\",\"title\":\"t(status)\",\"enum\":[\"a\",\"b\"]},"
      + "\"tags\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"enum\":[\"x\"]}}}}");
    JsonNode jsonForms = mapper.readTree("{\"type\":\"object\",\"properties\":{"
      + "\"tags\":{\"type\":\"array\",\"uniqueItems\":true,\"items\":{\"type\":\"string\",\"oneOf\":[{\"const\":\"x\",\"title\":\"X\"}]}},"
      + "\"status\":{\"type\":\"string\",\"title\":\"Status\",\"oneOf\":[{\"const\":\"a\"},{\"const\":\"b\"}]}}}");
    JsonNode other = mapper.readTree("{\"type\":\"object\",\"properties\":{"
      + "\"status\":{\"type\":\"integer\"},\"tags\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}");

    assertThat(StudiesImportResource.dataShape(asf)).isEqualTo(StudiesImportResource.dataShape(jsonForms));
    assertThat(StudiesImportResource.dataShape(asf)).isNotEqualTo(StudiesImportResource.dataShape(other));
  }
}
