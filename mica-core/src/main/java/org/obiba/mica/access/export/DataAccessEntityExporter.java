/*
 * Copyright (c) 2024 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.apache.commons.compress.utils.Lists;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.obiba.mica.micaConfig.service.helper.SchemaFormConfig;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class DataAccessEntityExporter {
  private static final Logger log = getLogger(DataAccessEntityExporter.class);

  private static final SimpleDateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final String[] skipTypes = {"fieldset","section"};

  private JsonNode schema;

  private JsonNode definition;

  private JsonNode model;

  private JSONObject wordConfig;

  private String prefix = "";

  private DataAccessEntityExporter() {
  }

  public ByteArrayOutputStream export(String titleStr, String status, String id) throws IOException {
    try (XWPFDocument document = new XWPFDocument();) {
      // document title
      XWPFParagraph title = document.createParagraph();
      title.setAlignment(ParagraphAlignment.LEFT);
      title.setSpacingAfter(400);
      XWPFRun titleRun = title.createRun();
      titleRun.setText(String.format("%s [%s] - %s", titleStr, status, id));
      applyFontConfig(titleRun, getItemConfig("documentTitle"));

      traverseDefinitionTree(document, definition, model);

      // footer
      XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
      XWPFParagraph footerParagraph = footer.createParagraph();
      XWPFRun footerRun = footerParagraph.createRun();
      footerRun.setText(String.format("%s [%s] - %s - %s", titleStr, status, id, ISO_8601.format(new Date())));
      applyFontConfig(footerRun, getItemConfig("footer"));

      ByteArrayOutputStream ba = new ByteArrayOutputStream();
      document.write(ba);
      return ba;
    }
  }

  /**
   * Traverse the definition items: object, array or text.
   *
   * @param document
   * @param node
   * @param modelObject
   */
  private void traverseDefinitionTree(XWPFDocument document, JsonNode node, JsonNode modelObject) {
    if (node.isObject()) {
      traverseObject(document, node, modelObject);
    } else if (node.isArray()) {
      for (JsonNode arrayElement : node) {
        // Recursively traverse array elements
        traverseDefinitionTree(document, arrayElement, modelObject);
      }
    } else if (node.isTextual()) {
      appendModelValue(document, node.asText(), node, modelObject);
    } else {
      log.warn("Unknown data access form definition node: {}", node);
    }
  }

  /**
   * Traverse a definition object.
   *
   * @param document
   * @param node
   * @param modelObject
   */
  private void traverseObject(XWPFDocument document, JsonNode node, JsonNode modelObject) {
    if (node.has("key")) {
      String key = node.get("key").asText();
      JsonNode keySchema = getKeySchema(key);
      if ("array".equals(keySchema.get("type").asText())) {
        appendModelValue(document, key, node, modelObject);
      } else if (node.has("items")) {
        traverseDefinitionTree(document, node.get("items"), modelObject);
      } else
        appendModelValue(document, node.get("key").asText(), node, modelObject);
    } else if (node.has("type")) {
      String type = node.get("type").asText();
      if ("help".equals(type)) {
        appendHelp(document, node);
      } else if ("tabs".equals(type)) {
        traverseDefinitionTree(document, node.get("tabs"), modelObject);
      } else if (node.has("items")) {
        traverseDefinitionTree(document, node.get("items"), modelObject);
      }
    } else if (node.has("items")) {
      appendHelp(document, node);
      traverseDefinitionTree(document, node.get("items"), modelObject);
    } else {
      log.warn("Unknown data access form definition node: {}", node);
    }
  }

  /**
   * Get the schema object for the provided key.
   *
   * @param key
   * @return
   */
  private JsonNode getKeySchema(String key) {
    if (key.contains("[]")) {
      List<String> keys = Splitter.on("[].").splitToList(key);
      List<JsonNode> nodes = Lists.newArrayList();
      for (String k : keys) {
        if (nodes.isEmpty()) {
          nodes.add(schema.get("properties").get(k));
        } else {
          nodes.add(nodes.get(nodes.size() - 1).get("items").get("properties").get(k));
        }
      }
      return nodes.get(nodes.size() - 1);
    }
    return schema.get("properties").get(key);
  }

  /**
   * Get the field name for the provided key (last token in the case of a list).
   *
   * @param key
   * @return
   */
  private String getKeyField(String key) {
    if (key.contains("[]")) {
      List<String> keys = Splitter.on("[].").splitToList(key);
      return keys.get(keys.size() - 1);
    }
    return key;
  }

  /**
   * Append title and help texts without HTML formatting.
   *
   * @param document
   * @param item
   */
  private void appendHelp(XWPFDocument document, JsonNode item) {
    if (item.has("title")) {
      String htmlText = item.get("title").asText();
      XWPFParagraph paragraph = document.createParagraph();
      addTextWithLineBreak(paragraph, Jsoup.parse(htmlText).wholeText(), getItemConfig(getHeading(htmlText, "title")));
    }
    if (item.has("helpvalue")) {
      String htmlText = item.get("helpvalue").asText();
      XWPFParagraph paragraph = document.createParagraph();
      addTextWithLineBreak(paragraph, Jsoup.parse(htmlText).wholeText(), getItemConfig(getHeading(htmlText, "help")));
    }
  }

  private String getHeading(String htmlText, String defaultKey) {
    for (int i = 1; i <= 6; i++) {
      String tag = "h" + i;
      if (htmlText.startsWith("<" + tag + ">")) {
        return tag;
      }
    }
    return defaultKey;
  }

  /**
   * Append model value as text, recursively if the value is an array.
   *
   * @param document
   * @param key
   * @param keyDescription
   * @param modelObject
   */
  private void appendModelValue(XWPFDocument document, String key, JsonNode keyDescription, JsonNode modelObject) {
    JsonNode keySchema = getKeySchema(key);
    if (keySchema == null) return;
    JsonNode value = modelObject.get(getKeyField(key));

    if (value == null) {
      if (getEmptyValueConfig().getBoolean("visible")) {
        appendKeyTitle(document, key);
        appendEmptyValue(document);
      }
    } else if (keySchema.has("type")) {
      appendKeyTitle(document, key);
      String type = keySchema.get("type").asText();
      if ("array".equals(type)) {
        if (keySchema.has("title")) {
          addLineBreak(document);
        }
        if ("string".equals(keySchema.get("items").get("type").asText())) {
          for (JsonNode itemValue : value) {
            addBulletedListItem(document, itemValue.asText());
          }
          addLineBreak(document);
        } else {
          List<JsonNode> itemsDescriptions = getItems(keyDescription);
          int count = 1;
          String savedPrefix = prefix;
          for (JsonNode itemValue : value) {
            prefix = Strings.isNullOrEmpty(savedPrefix) ? "" + count : String.format("%s.%s", savedPrefix, count);
            appendTitle(document, String.format("[%s]", prefix));
            for (JsonNode itemDescription : itemsDescriptions) {
              String itemKey = itemDescription.isTextual() ? itemDescription.asText() : itemDescription.get("key").asText();
              appendModelValue(document, itemKey, itemDescription, itemValue);
            }
            count++;
          }
          prefix = savedPrefix;
        }
      } else if ("object".equals(type)) {
        if (keySchema.has("format") && "obibaFiles".equals(keySchema.get("format").asText())) {
          appendModelValueAsFiles(document, value);
        }
      } else {
        appendModelValueAsText(document, keyDescription, value);
      }
    } else {
      appendKeyTitle(document, key);
      appendModelValueAsText(document, keyDescription, value);
    }
  }

  /**
   * Get the real list of child items, skipping the intermediate sections.
   *
   * @param node
   * @return
   */
  private List<JsonNode> getItems(JsonNode node) {
    List<JsonNode> items = Lists.newArrayList();
    if (node.has("items")) {
      for (JsonNode item : node.get("items")) {
        if (item.has("type") && Arrays.asList(skipTypes).contains(item.get("type").asText())) {
          items.addAll(getItems(item));
        } else {
          items.add(item);
        }
      }
    }
    return items;
  }

  /**
   * Append the file names from obibaFile objects.
   *
   * @param document
   * @param value
   */
  private void appendModelValueAsFiles(XWPFDocument document, JsonNode value) {
    if (!value.has("obibaFiles")) return;
    JsonNode files = value.get("obibaFiles");
    if (!files.isArray()) return;
    for (JsonNode file : files) {
      addBulletedListItem(document, file.get("fileName").asText());
    }
    addLineBreak(document);
  }

  /**
   * Append a text informing that there is a missing value.
   *
   * @param document
   */
  private void appendEmptyValue(XWPFDocument document) {
    JSONObject emptyValueConfig = getEmptyValueConfig();
    String nullText = emptyValueConfig.getString("nullText");
    XWPFParagraph paragraph = createParagraph(document);
    paragraph.setAlignment(ParagraphAlignment.LEFT);
    XWPFRun valueRun = paragraph.createRun();
    valueRun.setText(nullText);
    applyFontConfig(valueRun, getEmptyValueConfig());
    valueRun.addBreak();
  }

  /**
   * Append text as a field title.
   *
   * @param document
   * @param title
   */
  private void appendTitle(XWPFDocument document, String title) {
    XWPFParagraph paragraph = createParagraph(document);
    paragraph.setAlignment(ParagraphAlignment.LEFT);
    XWPFRun keyRun = paragraph.createRun();
    keyRun.setText(title);
    applyFontConfig(keyRun, getItemConfig("field"));
  }

  /**
   * Apply the  font configuration.
   *
   * @param run
   * @param itemConfig
   */
  private void applyFontConfig(XWPFRun run, JSONObject itemConfig) {
    if (itemConfig == null) {
      run.setFontFamily("Arial");
    } else if (itemConfig.has("style")) {
      // FIXME does not seem to work, provided style is not applied
      run.setStyle(getStyleConfig(itemConfig));
    } else if (itemConfig.has("font")) {
      JSONObject fontConfig = getFontConfig(itemConfig);
      run.setFontFamily(fontConfig.getString("family"));
      run.setFontSize(fontConfig.getInt("size"));
      run.setItalic(fontConfig.getBoolean("italic"));
      run.setBold(fontConfig.getBoolean("bold"));
      run.setColor(fontConfig.getString("color"));
    }
  }

  /**
   * Get the schema object from provided key and append its title if any.
   *
   * @param document
   * @param key
   */
  private void appendKeyTitle(XWPFDocument document, String key) {
    JsonNode keySchema = getKeySchema(key);
    if (keySchema == null || !keySchema.has("title")) return;
    String title = keySchema.get("title").asText();
    appendTitle(document, title);
  }

  /**
   * Append model value, use titleMap definition if any.
   *
   * @param document
   * @param keyDescription
   * @param value
   */
  private void appendModelValueAsText(XWPFDocument document, JsonNode keyDescription, JsonNode value) {
    XWPFParagraph valueParagraph = createParagraph(document);
    valueParagraph.setAlignment(ParagraphAlignment.LEFT);
    String txtValue = value.asText();
    if (keyDescription.has("titleMap")) {
      for (JsonNode map : keyDescription.get("titleMap")) {
        if (map.has("value") && map.has("name") && map.get("value").asText().equals(txtValue)) {
          txtValue = map.get("name").asText();
          break;
        }
      }
    }
    addTextWithLineBreak(valueParagraph, txtValue, getItemConfig("value"));
  }

  /**
   * Append text, including its line breaks.
   *
   * @param paragraph
   * @param text
   */
  private void addTextWithLineBreak(XWPFParagraph paragraph, String text) {
    addTextWithLineBreak(paragraph, text, null);
  }

  /**
   * Append text with a specified color and including its line breaks.
   *
   * @param paragraph
   * @param text
   * @param itemConfig
   */
  private void addTextWithLineBreak(XWPFParagraph paragraph, String text, JSONObject itemConfig) {
    Splitter.on("\n").split(text).forEach(str -> {
      XWPFRun run = paragraph.createRun();
      run.setText(str);
      applyFontConfig(run, itemConfig);
      run.addBreak();
    });
  }

  /**
   * Add a bulleted list item.
   *
   * @param document
   * @param listItem
   */
  private void addBulletedListItem(XWPFDocument document, String listItem) {
    XWPFParagraph paragraph = createParagraph(document);
    paragraph.setNumID(addBulletNumbering(document));
    XWPFRun run = paragraph.createRun();
    run.setText(listItem);
  }

  /**
   * Register bullet number in document.
   *
   * @param document
   * @return
   */
  private BigInteger addBulletNumbering(XWPFDocument document) {
    // Create a numbering instance and set the bullet style
    CTAbstractNum cTAbstractNum = CTAbstractNum.Factory.newInstance();
    cTAbstractNum.setAbstractNumId(BigInteger.valueOf(0));
    CTLvl cTLvl = cTAbstractNum.addNewLvl();
    cTLvl.setIlvl(BigInteger.valueOf(0));
    cTLvl.addNewNumFmt().setVal(STNumberFormat.BULLET);
    cTLvl.addNewLvlText().setVal("•");
    XWPFAbstractNum abstractNum = new XWPFAbstractNum(cTAbstractNum);
    BigInteger oldAbstractNum = document.createNumbering().addAbstractNum(abstractNum);

    // Create a numbering level reference
    return document.getNumbering().addNum(oldAbstractNum);
  }

  /**
   * Add a line break to the document.
   *
   * @param document
   */
  private void addLineBreak(XWPFDocument document) {
    XWPFParagraph paragraph = createParagraph(document);
    //XWPFRun run = paragraph.createRun();
    //run.addBreak();
  }

  /**
   * Use the prefix string to count the number of indentations to apply.
   *
   * @return
   */
  private int getIndentation() {
    return Strings.isNullOrEmpty(prefix) ? 0 : prefix.split("\\.").length;
  }

  /**
   * Create a paragraph and apply indentations if any.
   *
   * @param document
   * @return
   */
  private XWPFParagraph createParagraph(XWPFDocument document) {
    XWPFParagraph paragraph = document.createParagraph();
    paragraph.setIndentationLeft(getIndentation() * 360);
    return paragraph;
  }

  private JSONObject getEmptyValueConfig() {
    try {
      return wordConfig.getJSONObject("emptyValue");
    } catch (JSONException e) {
      return new JSONObject("{" +
        "  \"visible\": false," +
        "  \"nullText\": \"N/A\"" +
        "}");
    }
  }

  private JSONObject getItemConfig(String key) {
    try {
      return wordConfig.getJSONObject(key);
    } catch (JSONException e) {
      return new JSONObject("{}");
    }
  }

  private String getStyleConfig(JSONObject config) {
    return config.has("style") ? config.getString("style") : "";
  }

  private JSONObject getFontConfig(JSONObject config) {
    try {
      return config.getJSONObject("font");
    } catch (JSONException e) {
      return new JSONObject("{\n" +
        "  \"family\": \"Arial\",\n" +
        "  \"size\": 10,\n" +
        "  \"italic\": false,\n" +
        "  \"bold\": false,\n" +
        "  \"color\": \"000000\"\n" +
        "}");
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private final DataAccessEntityExporter exporter;

    private ObjectMapper objectMapper = new ObjectMapper();

    public Builder() {
      exporter = new DataAccessEntityExporter();
    }

    public Builder config(SchemaFormConfig config, JSONObject wordConfig) {
      schema(config.getSchema());
      definition(config.getDefinition());
      model(config.getModel());
      this.exporter.wordConfig = wordConfig;
      return this;
    }

    public Builder form(AbstractDataAccessEntityForm form) {
      schema(form.getSchema());
      definition(form.getDefinition());
      return this;
    }

    public Builder entity(DataAccessEntity entity) {
      model(entity.getContent());
      return this;
    }

    public Builder schema(String schema) {
      try {
        this.exporter.schema = objectMapper.readTree(schema);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    public Builder definition(String definition) {
      try {
        this.exporter.definition = objectMapper.readTree(definition);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    public Builder model(String model) {
      try {
        this.exporter.model = objectMapper.readTree(Strings.isNullOrEmpty(model) ? "{}" : model);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    public DataAccessEntityExporter build() {
      return exporter;
    }

  }

}
