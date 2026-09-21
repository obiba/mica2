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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * A JSON Forms UI schema as the definition tree walked by {@link DataAccessEntityExporter}, which was
 * written for the angular-schema-form dialect: a {@code Control} is a keyed item ({@code a.b} for the
 * scope {@code #/properties/a/properties/b}, {@code list[].x} for a control of the item UI schema of a
 * list), a {@code Label} a help block, a labelled {@code Group} or {@code Category} a titled section,
 * any other layout a plain section. A list of objects without an item UI schema ({@code options.items})
 * is rendered by JSON Forms with one control per property of its item schema: its keyed items come from
 * the schema.
 */
final class UischemaDefinition {

  private static final String SCOPE_PREFIX = "#/";

  private static final String PROPERTIES = "properties";

  private static final String ITEMS = "items";

  private UischemaDefinition() {}

  /**
   * The definition tree of the UI schema, in the angular-schema-form shape.
   *
   * @param uischema the JSON Forms UI schema
   * @param schema the JSON schema of the form, for the lists rendered without an item UI schema (may be null)
   */
  static ArrayNode toDefinition(JsonNode uischema, JsonNode schema) {
    ArrayNode items = JsonNodeFactory.instance.arrayNode();
    if (uischema != null && uischema.isObject()) addElement(items, uischema, "", schema);
    return items;
  }

  private static void addElements(ArrayNode items, JsonNode element, String keyPrefix, JsonNode schema) {
    JsonNode elements = element.get("elements");
    if (elements == null || !elements.isArray()) return;
    for (JsonNode child : elements) {
      addElement(items, child, keyPrefix, schema);
    }
  }

  /**
   * @param keyPrefix the key prefix of the controls ({@code list[].} in the item UI schema of a list)
   * @param schema the schema the scopes of the controls are relative to (the item schema in the item UI schema of a list)
   */
  private static void addElement(ArrayNode items, JsonNode element, String keyPrefix, JsonNode schema) {
    if (!element.isObject()) return;
    String type = element.has("type") ? element.get("type").asText() : "";
    switch (type) {
      case "Control":
        addControl(items, element, keyPrefix, schema);
        break;
      case "Label":
        if (element.has("text")) {
          ObjectNode help = items.addObject();
          help.put("type", "help");
          help.put("helpvalue", element.get("text").asText());
        }
        break;
      default:
        ObjectNode section = items.addObject();
        String label = labelOf(element);
        if (Strings.isNullOrEmpty(label)) {
          section.put("type", "section");
        } else {
          section.put("title", label);
        }
        addElements(section.putArray(ITEMS), element, keyPrefix, schema);
        break;
    }
  }

  private static void addControl(ArrayNode items, JsonNode control, String keyPrefix, JsonNode schema) {
    String key = keyOf(control.get("scope"), keyPrefix);
    if (key == null) return;
    ObjectNode item = items.addObject();
    item.put("key", key);
    JsonNode itemSchema = itemSchemaOf(schemaOf(schema, control.get("scope")));
    JsonNode options = control.get("options");
    if (options != null && options.has(ITEMS) && options.get(ITEMS).isObject()) {
      // the UI schema of one item of the list, its scopes relative to the item
      addElement(item.putArray(ITEMS), options.get(ITEMS), key + "[].", itemSchema);
    } else if (itemSchema != null) {
      // the default item layout: one control per property of the item
      addProperties(item.putArray(ITEMS), itemSchema, key + "[].");
    }
  }

  /** the keyed items of the properties of an object schema, the lists of objects among them with their own items */
  private static void addProperties(ArrayNode items, JsonNode objectSchema, String keyPrefix) {
    objectSchema.get(PROPERTIES).fields().forEachRemaining(property -> {
      ObjectNode item = items.addObject();
      String key = keyPrefix + property.getKey();
      item.put("key", key);
      JsonNode itemSchema = itemSchemaOf(property.getValue());
      if (itemSchema != null) addProperties(item.putArray(ITEMS), itemSchema, key + "[].");
    });
  }

  /** the schema of the items of a list of objects, null for anything else */
  private static JsonNode itemSchemaOf(JsonNode schema) {
    if (schema == null || !schema.isObject()) return null;
    JsonNode itemSchema = schema.get(ITEMS);
    return itemSchema != null && itemSchema.isObject() && itemSchema.has(PROPERTIES) && itemSchema.get(PROPERTIES).isObject()
      ? itemSchema
      : null;
  }

  /** the schema of a scope ({@code #/properties/a/properties/b}), null when unknown */
  static JsonNode schemaOf(JsonNode schema, JsonNode scope) {
    if (schema == null || scope == null || !scope.isTextual()) return null;
    String path = scope.asText();
    if (!path.startsWith(SCOPE_PREFIX)) return null;
    JsonNode current = schema;
    for (String segment : path.substring(SCOPE_PREFIX.length()).split("/")) {
      if (segment.isEmpty()) continue;
      if (current == null || !current.isObject()) return null;
      current = current.get(segment);
    }
    return current == schema ? null : current;
  }

  /** the label of a group or category, when it has one ({@code label: false} hides it) */
  private static String labelOf(JsonNode element) {
    for (String name : new String[]{"label", "title"}) {
      JsonNode label = element.get(name);
      if (label != null && label.isTextual()) return label.asText();
    }
    return null;
  }

  /**
   * The dotted key of a scope: {@code #/properties/a/properties/b} is {@code a.b}. The scope of the item
   * itself ({@code #}) has no key.
   */
  static String keyOf(JsonNode scope, String keyPrefix) {
    if (scope == null || !scope.isTextual()) return null;
    String path = scope.asText();
    if (!path.startsWith(SCOPE_PREFIX)) return null;
    List<String> parts = new ArrayList<>();
    String[] segments = path.substring(SCOPE_PREFIX.length()).split("/");
    for (int i = 0; i < segments.length; i++) {
      if (i % 2 == 0) {
        // the even segments are "properties"
        if (!PROPERTIES.equals(segments[i])) return null;
      } else if (!segments[i].isEmpty()) {
        parts.add(segments[i]);
      }
    }
    if (parts.isEmpty()) return null;
    return keyPrefix + String.join(".", parts);
  }
}
