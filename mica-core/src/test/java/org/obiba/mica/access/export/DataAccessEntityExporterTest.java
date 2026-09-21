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

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.assertj.core.util.Strings;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataAccessEntityExporterTest {

  private static final String SCHEMA = "{\"type\": \"object\", \"properties\": {" +
    "\"name\": {\"type\": \"string\", \"title\": \"Name\"}," +
    "\"career\": {\"type\": \"string\", \"title\": \"Career\", \"oneOf\": [{\"const\": \"student\", \"title\": \"Student\"}, {\"const\": \"senior\", \"title\": \"Senior\"}]}," +
    "\"tags\": {\"type\": \"array\", \"title\": \"Tags\", \"items\": {\"type\": \"string\", \"oneOf\": [{\"const\": \"a\", \"title\": \"Alpha\"}, {\"const\": \"b\", \"title\": \"Beta\"}]}}," +
    "\"staff\": {\"type\": \"array\", \"title\": \"Staff\", \"items\": {\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\", \"title\": \"Member\"}}}}" +
    "}}";

  private static final String UISCHEMA = "{\"type\": \"VerticalLayout\", \"elements\": [" +
    "{\"type\": \"Label\", \"text\": \"<h2>Intro</h2><p>Some help</p>\"}," +
    "{\"type\": \"Group\", \"label\": \"Applicant\", \"elements\": [" +
    "  {\"type\": \"Control\", \"scope\": \"#/properties/name\"}," +
    "  {\"type\": \"Control\", \"scope\": \"#/properties/career\", \"options\": {\"format\": \"radio\"}}," +
    "  {\"type\": \"Control\", \"scope\": \"#/properties/tags\"}" +
    "]}," +
    "{\"type\": \"Control\", \"scope\": \"#/properties/staff\", \"options\": {\"items\": {\"type\": \"VerticalLayout\", \"elements\": [{\"type\": \"Control\", \"scope\": \"#/properties/name\"}]}}}" +
    "]}";

  private static final String MODEL = "{\"name\": \"Jane\", \"career\": \"student\", \"tags\": [\"b\", \"c\"], \"staff\": [{\"name\": \"Joe\"}, {\"name\": \"Ann\"}]}";

  private String defaultSchema;

  private String defaultUischema;

  @Test
  public void test_empty_builder() throws IOException {
    DataAccessEntityExporter exporter = DataAccessEntityExporter.newBuilder()
      .schema("{}")
      .definition("[]")
      .model("{}")
      .wordConfig(wordConfig())
      .build();
    exporter.export("Test", "Approved", "000");
  }

  /** the document configuration of the default application form */
  private JSONObject wordConfig() throws IOException {
    return new JSONObject(readFileFromClasspath("config/data-access-form/export-word.json"));
  }

  @Test
  public void test_asf_definition() throws IOException {
    // the definition of a form revision from before the JSON Forms dialect
    DataAccessEntityExporter exporter = DataAccessEntityExporter.newBuilder()
      .schema(readFileFromClasspath("config/data-access-form-asf/schema.json"))
      .definition(readFileFromClasspath("config/data-access-form-asf/definition.json"))
      .model("{}")
      .wordConfig(wordConfig())
      .build();
    exporter.export("Test", "Approved", "000");
  }

  @Test
  public void test_default_uischema() throws IOException {
    DataAccessEntityExporter exporter = DataAccessEntityExporter.newBuilder()
      .schema(getDefaultSchema())
      .definition(getDefaultUischema())
      .model("{\"name\": \"Jane\", \"staff\": [{\"name\": \"Joe\", \"title\": \"Dr\"}]}")
      .wordConfig(wordConfig())
      .build();
    String text = text(exporter.export("Test", "Approved", "000"));
    assertTrue(text.contains("Jane"), text);
    assertTrue(text.contains("Joe"), text);
  }

  @Test
  public void test_uischema_labels_and_values() throws IOException {
    DataAccessEntityExporter exporter = DataAccessEntityExporter.newBuilder()
      .schema(SCHEMA)
      .definition(UISCHEMA)
      .model(MODEL)
      .wordConfig(wordConfig())
      .build();
    String text = text(exporter.export("Test", "Approved", "000"));
    assertTrue(text.contains("Intro"), text);
    assertTrue(text.contains("Some help"), text);
    // a plain text group label
    assertTrue(text.contains("Applicant"), text);
    assertTrue(text.contains("Name"), text);
    assertTrue(text.contains("Jane"), text);
    // the oneOf titles, of a value and of the items of a list
    assertTrue(text.contains("Student"), text);
    assertTrue(text.contains("Beta"), text);
    assertTrue(text.contains("c"), text);
    // the items of a list of objects
    assertTrue(text.contains("Joe"), text);
    assertTrue(text.contains("Ann"), text);
    assertTrue(text.contains("Member"), text);
  }

  @Test
  public void test_asf_title_map() throws IOException {
    String schema = "{\"type\": \"object\", \"properties\": {\"career\": {\"type\": \"string\", \"title\": \"Career\", \"enum\": [\"student\", \"senior\"]}}}";
    String definition = "[{\"key\": \"career\", \"type\": \"radios\", \"titleMap\": [{\"value\": \"student\", \"name\": \"Student\"}, {\"value\": \"senior\", \"name\": \"Senior\"}]}]";
    DataAccessEntityExporter exporter = DataAccessEntityExporter.newBuilder()
      .schema(schema)
      .definition(definition)
      .model("{\"career\": \"senior\"}")
      .wordConfig(wordConfig())
      .build();
    String text = text(exporter.export("Test", "Approved", "000"));
    assertTrue(text.contains("Senior"), text);
  }

  /** the texts of the paragraphs and of the table cells of the document */
  private String text(ByteArrayOutputStream out) throws IOException {
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(out.toByteArray()))) {
      StringBuilder text = new StringBuilder();
      document.getParagraphs().forEach(p -> text.append(p.getText()).append("\n"));
      document.getTables().forEach(t -> t.getRows().forEach(r -> r.getTableCells().forEach(c -> text.append(c.getText()).append("\n"))));
      return text.toString();
    }
  }

  private String getDefaultSchema() throws IOException  {
    if (Strings.isNullOrEmpty(defaultSchema))
      defaultSchema = readFileFromClasspath("config/data-access-form/schema.json");
    return defaultSchema;
  }

  private String getDefaultUischema() throws IOException  {
    if (Strings.isNullOrEmpty(defaultUischema))
      defaultUischema = readFileFromClasspath("config/data-access-form/uischema.json");
    return defaultUischema;
  }

  private String readFileFromClasspath(String filePath) throws IOException {
    // Use the class loader to get the resource as an InputStream
    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(filePath);

    if (inputStream != null) {
      // Read the content of the InputStream into a string
      try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
        return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
      }
    } else {
      throw new IOException("File not found: " + filePath);
    }
  }
}
