/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UischemaDefinitionTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void scope_keys() {
    assertEquals("name", UischemaDefinition.keyOf(new TextNode("#/properties/name"), ""));
    assertEquals("address.city", UischemaDefinition.keyOf(new TextNode("#/properties/address/properties/city"), ""));
    assertEquals("staff[].name", UischemaDefinition.keyOf(new TextNode("#/properties/name"), "staff[]."));
    assertNull(UischemaDefinition.keyOf(new TextNode("#"), "tags[]."));
    assertNull(UischemaDefinition.keyOf(new TextNode("#/definitions/x"), ""));
    assertNull(UischemaDefinition.keyOf(null, ""));
  }

  @Test
  public void uischema_as_a_definition_tree() throws Exception {
    JsonNode uischema = mapper.readTree("{" +
      "\"type\": \"VerticalLayout\", \"elements\": [" +
      "  {\"type\": \"Label\", \"text\": \"<h2>Intro</h2>\"}," +
      "  {\"type\": \"Group\", \"label\": \"Applicant\", \"elements\": [" +
      "    {\"type\": \"HorizontalLayout\", \"elements\": [{\"type\": \"Control\", \"scope\": \"#/properties/name\"}]}," +
      "    {\"type\": \"Control\", \"scope\": \"#/properties/staff\", \"options\": {\"items\": {" +
      "      \"type\": \"VerticalLayout\", \"elements\": [{\"type\": \"Control\", \"scope\": \"#/properties/name\"}, {\"type\": \"Label\", \"text\": \"note\"}]" +
      "    }}}" +
      "  ]}," +
      "  {\"type\": \"Categorization\", \"elements\": [{\"type\": \"Category\", \"label\": \"Tab\", \"elements\": [{\"type\": \"Control\", \"scope\": \"#/properties/summary\"}]}]}," +
      "  {\"type\": \"Control\", \"scope\": \"#\"}" +
      "]}");

    ArrayNode definition = UischemaDefinition.toDefinition(uischema);
    assertEquals(1, definition.size());
    JsonNode root = definition.get(0);
    assertEquals("section", root.get("type").asText());
    JsonNode items = root.get("items");
    assertEquals(3, items.size());

    assertEquals("help", items.get(0).get("type").asText());
    assertEquals("<h2>Intro</h2>", items.get(0).get("helpvalue").asText());

    JsonNode group = items.get(1);
    assertEquals("Applicant", group.get("title").asText());
    assertFalse(group.has("type"));
    JsonNode layout = group.get("items").get(0);
    assertEquals("section", layout.get("type").asText());
    assertEquals("name", layout.get("items").get(0).get("key").asText());
    JsonNode staff = group.get("items").get(1);
    assertEquals("staff", staff.get("key").asText());
    JsonNode staffItems = staff.get("items").get(0).get("items");
    assertEquals("staff[].name", staffItems.get(0).get("key").asText());
    assertEquals("help", staffItems.get(1).get("type").asText());

    JsonNode tabs = items.get(2);
    assertEquals("section", tabs.get("type").asText());
    assertEquals("Tab", tabs.get("items").get(0).get("title").asText());
    assertEquals("summary", tabs.get("items").get(0).get("items").get(0).get("key").asText());
  }

  @Test
  public void not_a_uischema() throws Exception {
    assertEquals(0, UischemaDefinition.toDefinition(null).size());
    assertEquals(0, UischemaDefinition.toDefinition(mapper.readTree("[]")).size());
    assertEquals(0, UischemaDefinition.toDefinition(mapper.readTree("{}")).get(0).get("items").size());
  }
}
