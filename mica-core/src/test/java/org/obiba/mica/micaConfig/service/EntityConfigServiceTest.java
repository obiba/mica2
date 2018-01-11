/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EntityConfigServiceTest {

  @Test
  public void can_merge_two_schemas() throws Exception {

    // Given
    String customSchema = "{" +
      "    \"type\": \"object\"," +
      "    \"properties\": {" +
      "        \"info\": {" +
      "            \"title\": \"info\"" +
      "        }," +
      "        \"info2\": {" +
      "            \"title\": \"info2\"" +
      "        }" +
      "    }," +
      "    \"required\": [" +
      "        \"info\"," +
      "        \"neededInfo\"" +
      "    ]" +
      "}";

    String mandatorySchema = "{" +
      "    \"type\": \"object\"," +
      "    \"properties\": {" +
      "        \"neededInfo\": {" +
      "            \"title\": \"neededInfo\"," +
      "            \"type\": \"string\"" +
      "        }," +
      "        \"info2\": {" +
      "            \"title\": \"info2Overriden\"" +
      "        }" +
      "    }," +
      "    \"required\": [" +
      "        \"neededInfo\"," +
      "        \"neededInfo2\"" +
      "    ]" +
      "}";

    // Execute
    String mergeSchema = new IndividualStudyConfigService().mergeSchema(customSchema, mandatorySchema);

    // Verify
    DocumentContext parse = JsonPath.parse(mergeSchema);
    assertThat(parse.read("type"), is("object"));
    assertThat(parse.read("properties.info.title"), is("info"));
    assertThat(parse.read("properties.info2.title"), is("info2Overriden"));
    assertThat(parse.read("required"), containsInAnyOrder("neededInfo", "neededInfo2", "info"));
  }

  @Test
  public void can_merge_two_definition() throws Exception {

    // Given
    String customDefinition = "[" +
      "    {" +
      "        \"type\": \"fieldset\"," +
      "        \"items\": [ \"customItem1\", \"customItem2\"]" +
      "    }" +
      "]";

    String mandatoryDefinition = "[" +
      "    {" +
      "        \"type\": \"fieldset\"," +
      "        \"items\": [\"mandatoryItem1\"]" +
      "    }" +
      "]";

    // Execute
    String mergeSchema = new IndividualStudyConfigService().mergeDefinition(customDefinition, mandatoryDefinition);

    // Verify
    DocumentContext parse = JsonPath.parse(mergeSchema);
    assertThat(parse.read("$[0].items[0]"), is("mandatoryItem1"));
    assertThat(parse.read("$[1].items[0]"), is("customItem1"));
  }
}
