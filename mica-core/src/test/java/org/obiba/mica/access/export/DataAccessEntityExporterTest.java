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

import org.assertj.core.util.Strings;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class DataAccessEntityExporterTest {

  private String defaultSchema;

  private String defaultDefinition;

  @Test
  public void test_empty_builder() {
    DataAccessEntityExporter exporter = DataAccessEntityExporter.newBuilder()
      .schema("{}")
      .definition("[]")
      .model("{}")
      .build();
  }

  @Test
  public void test_sf() throws IOException {
    DataAccessEntityExporter exporter = DataAccessEntityExporter.newBuilder()
      .schema(getDefaultSchema())
      .definition(getDefaultDefinition())
      .model("{}")
      .build();
    exporter.export("Test", "000");
  }

  private String getDefaultSchema() throws IOException  {
    if (Strings.isNullOrEmpty(defaultSchema))
      defaultSchema = readFileFromClasspath("config/data-access-form/schema.json");
    return defaultSchema;
  }
  private String getDefaultDefinition() throws IOException  {
    if (Strings.isNullOrEmpty(defaultDefinition))
      defaultDefinition = readFileFromClasspath("config/data-access-form/definition.json");
    return defaultDefinition;
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
