/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The built-in pages put the Markdown source of a document field in a template block, rendered and sanitized in the
 * browser. A value printed there without templateSafe() can close the block and be parsed as live HTML.
 */
public class TemplatesMarkdownBlockTest {

  // a plain template block; Vue ones have attributes (v-if, v-else...)
  private static final Pattern TEMPLATE_BLOCK = Pattern.compile("<template>(.*?)</template>", Pattern.DOTALL);

  private static final Pattern INTERPOLATION = Pattern.compile("\\$\\{");

  private static final Pattern SAFE_INTERPOLATION = Pattern.compile("\\$\\{templateSafe\\(");

  @Test
  public void test_every_value_in_a_template_block_goes_through_templateSafe() throws IOException, URISyntaxException {
    Path root = Path.of(getClass().getResource("/_templates/libs/head.ftl").toURI()).getParent().getParent();
    List<String> unsafe = new ArrayList<>();
    int blocks = 0;
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".ftl")).toList()) {
        Matcher block = TEMPLATE_BLOCK.matcher(Files.readString(file, StandardCharsets.UTF_8));
        while (block.find()) {
          String content = block.group(1);
          long interpolations = INTERPOLATION.matcher(content).results().count();
          if (interpolations == 0) continue;
          blocks++;
          if (SAFE_INTERPOLATION.matcher(content).results().count() != interpolations) {
            unsafe.add(root.relativize(file) + ": " + block.group().strip());
          }
        }
      }
    }
    // makes sure the blocks are found
    assertThat(blocks).isGreaterThan(30);
    assertThat(unsafe).as("values in a <template> block not wrapped with templateSafe()").isEmpty();
  }
}
