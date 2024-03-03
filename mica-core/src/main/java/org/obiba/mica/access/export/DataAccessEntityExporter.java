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
import org.jsoup.Jsoup;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.obiba.mica.micaConfig.service.helper.SchemaFormConfig;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DataAccessEntityExporter {

  private static final SimpleDateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private JsonNode schema;

  private JsonNode definition;

  private JsonNode model;

  private DataAccessEntityExporter() {
  }

  public ByteArrayOutputStream export(String titleStr, String id) throws IOException {
    try (XWPFDocument document = new XWPFDocument();) {
      XWPFParagraph title = document.createParagraph();
      title.setAlignment(ParagraphAlignment.LEFT);
      title.setSpacingAfter(400);
      XWPFRun titleRun = title.createRun();
      titleRun.setText(String.format("%s - %s", titleStr, id));
      titleRun.setBold(true);
      titleRun.setFontSize(20);

      traverseJsonTree(document, definition);

      XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
      XWPFParagraph footerParagraph = footer.createParagraph();
      XWPFRun footerRun = footerParagraph.createRun();
      footerRun.setFontSize(10);
      footerRun.setColor("757575");
      footerRun.setText(String.format("%s - %s - %s", titleStr, id, ISO_8601.format(new Date())));

      ByteArrayOutputStream ba = new ByteArrayOutputStream();
      document.write(ba);
      return ba;
    }
  }

  private void traverseJsonTree(XWPFDocument document, JsonNode node) {
    if (node.isObject()) {
      if (node.has("key")) {
        if (node.has("items")) {
          traverseJsonTree(document, node.get("items"));
        } else
          appendModelValue(document, node.get("key").asText(), node);
      } else if (node.has("type")) {
        String type = node.get("type").asText();
        if ("help".equals(type)) {
          appendHelp(document, node);
        } else if (node.has("items")) {
          traverseJsonTree(document, node.get("items"));
        }
      }
    } else if (node.isArray()) {
      for (JsonNode arrayElement : node) {
        // Recursively traverse array elements
        traverseJsonTree(document, arrayElement);
      }
    } else if (node.isTextual()) {
      appendModelValue(document, node.asText(), null);
    } else {
      System.out.println("Value: " + node.asText());
    }
  }

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

  private void appendHelp(XWPFDocument document, JsonNode item) {
    XWPFParagraph paragraph = document.createParagraph();
    addTextWithLineBreak(paragraph, Jsoup.parse(item.get("helpvalue").asText()).wholeText(), "757575");
  }

  private void appendModelValue(XWPFDocument document, String key, JsonNode keyDescription) {
    XWPFParagraph keyParagraph = document.createParagraph();
    keyParagraph.setAlignment(ParagraphAlignment.LEFT);

    JsonNode keySchema = getKeySchema(key);
    if (keySchema == null) return;
    String title = String.format("[%s]", key);
    if (keySchema.has("title")) {
      title = keySchema.get("title").asText();
    }
    XWPFRun keyRun = keyParagraph.createRun();
    keyRun.setText(title);
    keyRun.setFontFamily("Courier");
    keyRun.setFontSize(10);
    keyRun.setBold(true);

    JsonNode value = model.get(key);
    if (value == null) {
      XWPFParagraph valueParagraph = document.createParagraph();
      valueParagraph.setAlignment(ParagraphAlignment.LEFT);
      XWPFRun valueRun = valueParagraph.createRun();
      valueRun.setText("N/A");
      valueRun.setFontSize(8);
      valueRun.setItalic(true);
      valueRun.setColor("666666");
      valueRun.addBreak();
    } else if (keySchema.has("type")) {
      String type = keySchema.get("type").asText();
      if ("array".equals(type)) {
        JsonNode arrayProperties = keySchema.get("items").get("properties");

      } else if ("object".equals(type)) {
        if (keySchema.has("format") && "obibaFiles".equals(keySchema.get("format").asText())) {
          appendModelValueAsFiles(document, value);
        }
      } else {
        appendModelValueAsText(document, value);
      }
    } else {
      appendModelValueAsText(document, value);
    }
  }

  private void appendModelValueAsFiles(XWPFDocument document, JsonNode value) {
    if (!value.has("obibaFiles")) return;
    JsonNode files = value.get("obibaFiles");
    if (!files.isArray()) return;
    for (JsonNode file : files) {
      addBulletedListItem(document, file.get("fileName").asText());
    }
    XWPFParagraph paragraph = document.createParagraph();
    XWPFRun run = paragraph.createRun();
    run.addBreak();
  }

  private void appendModelValueAsText(XWPFDocument document, JsonNode value) {
    XWPFParagraph valueParagraph = document.createParagraph();
    valueParagraph.setAlignment(ParagraphAlignment.LEFT);
    addTextWithLineBreak(valueParagraph, value.asText());
  }

  private void addTextWithLineBreak(XWPFParagraph paragraph, String text) {
    addTextWithLineBreak(paragraph, text, null);
  }

  private void addTextWithLineBreak(XWPFParagraph paragraph, String text, String color) {
    Splitter.on("\n").split(text).forEach(str -> {
      XWPFRun run = paragraph.createRun();
      run.setText(str);
      if (!Strings.isNullOrEmpty(color)) run.setColor(color);
      run.addBreak();
    });
  }

  private void addBulletedListItem(XWPFDocument document, String listItem) {
    // Create a paragraph
    XWPFParagraph paragraph = document.createParagraph();

    // Set paragraph style to bullet
    paragraph.setNumID(addBulletNumbering(document));

    // Add text to the paragraph
    XWPFRun run = paragraph.createRun();
    run.setText(listItem);
  }

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

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private final DataAccessEntityExporter exporter;

    private ObjectMapper objectMapper = new ObjectMapper();

    public Builder() {
      exporter = new DataAccessEntityExporter();
    }

    public Builder config(SchemaFormConfig config) {
      schema(config.getSchema());
      definition(config.getDefinition());
      model(config.getModel());
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
        this.exporter.model = objectMapper.readTree(model);
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
