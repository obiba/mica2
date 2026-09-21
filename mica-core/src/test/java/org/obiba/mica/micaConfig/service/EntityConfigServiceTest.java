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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

  @Test
  public void can_merge_two_uischemas() throws Exception {

    // Given
    String customUischema = "{" +
      "    \"type\": \"VerticalLayout\"," +
      "    \"elements\": [ { \"type\": \"Control\", \"scope\": \"#/properties/customItem1\" } ]" +
      "}";

    String mandatoryUischema = "{" +
      "    \"type\": \"VerticalLayout\"," +
      "    \"elements\": [ { \"type\": \"Control\", \"scope\": \"#/properties/mandatoryItem1\" } ]" +
      "}";

    // Execute
    String merged = new IndividualStudyConfigService().mergeUischema(customUischema, mandatoryUischema);

    // Verify
    DocumentContext parse = JsonPath.parse(merged);
    assertThat(parse.read("type"), is("VerticalLayout"));
    assertThat(parse.read("elements[0].scope"), is("#/properties/mandatoryItem1"));
    assertThat(parse.read("elements[1].type"), is("VerticalLayout"));
    assertThat(parse.read("elements[1].elements[0].scope"), is("#/properties/customItem1"));
  }

  @Test
  public void can_merge_uischema_without_mandatory_one() throws Exception {

    // Given
    String customUischema = "{ \"type\": \"VerticalLayout\", \"elements\": [] }";

    // Execute
    String merged = new IndividualStudyConfigService().mergeUischema(customUischema, "{\"type\":\"VerticalLayout\",\"elements\":[]}");

    // Verify
    DocumentContext parse = JsonPath.parse(merged);
    assertThat(parse.read("elements[0].type"), is("VerticalLayout"));
  }

  @Test
  public void can_merge_schema_without_required_and_properties() throws Exception {

    // Given: a custom schema as the form builder writes it, without an empty required array
    String customSchema = "{ \"type\": \"object\" }";

    String mandatorySchema = "{" +
      "    \"type\": \"object\"," +
      "    \"properties\": { \"neededInfo\": { \"type\": \"string\" } }," +
      "    \"required\": [ \"neededInfo\" ]" +
      "}";

    // Execute
    String mergeSchema = new IndividualStudyConfigService().mergeSchema(customSchema, mandatorySchema);

    // Verify
    DocumentContext parse = JsonPath.parse(mergeSchema);
    assertThat(parse.read("properties.neededInfo.type"), is("string"));
    assertThat(parse.read("required"), containsInAnyOrder("neededInfo"));
  }

  @Test
  public void every_document_type_has_a_mandatory_uischema() throws Exception {

    // Given: the form configuration services of the document types edited with the form builder
    List<EntityConfigService<?>> services = List.of(
      new NetworkConfigService(),
      new IndividualStudyConfigService(),
      new HarmonizationStudyConfigService(),
      new PopulationConfigService(),
      new DataCollectionEventConfigService(),
      new StudyDatasetConfigService(),
      new HarmonizationDatasetConfigService(),
      new ProjectConfigService());

    for (EntityConfigService<?> service : services) {
      // Execute
      String path = service.getMandatoryUischemaResourcePath();
      String merged = service.mergeUischema("{}", readResource(path));

      // Verify: a layout with the mandatory controls, merged with an empty custom form
      DocumentContext parse = JsonPath.parse(merged);
      assertThat(path, parse.read("type"), is("VerticalLayout"));
      assertThat(path, parse.read("elements"), is(not(empty())));

      // Verify: every property of the mandatory schema has its control
      JsonNode schema = new ObjectMapper().readTree(readResource(service.getMandatorySchemaResourcePath()));
      List<String> scopes = controlScopes(new ObjectMapper().readTree(merged), new ArrayList<>());
      schema.get("properties").fieldNames()
        .forEachRemaining(property -> assertThat(path, scopes, hasItem("#/properties/" + property)));
    }
  }

  private String readResource(String path) throws IOException {
    Resource resource = new DefaultResourceLoader().getResource(path);
    assertThat(path, resource.exists(), is(true));
    try (InputStream in = resource.getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** the scopes of the controls of a UI schema, layouts included */
  private List<String> controlScopes(JsonNode uischema, List<String> scopes) {
    if (uischema.hasNonNull("scope")) scopes.add(uischema.get("scope").asText());
    if (uischema.has("elements")) uischema.get("elements").forEach(element -> controlScopes(element, scopes));
    return scopes;
  }
}
