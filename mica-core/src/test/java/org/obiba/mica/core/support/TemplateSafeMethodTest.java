/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.support;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateModelException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TemplateSafeMethodTest {

  private static final String PAYLOAD = "<img src=x onerror=alert(1)>";

  // Markdown as typed, with characters that must reach the Markdown renderer unchanged
  private static final String MARKDOWN = "**bold** <i>html</i> a < b & c > d \"q\" 'q' [link](http://x.org?a=1&b=2)";

  @Test
  public void test_neutralize_closing_template_tag() {
    assertThat(TemplateSafeMethod.neutralize("a</template>" + PAYLOAD)).isEqualTo("a&lt;/template>" + PAYLOAD);
  }

  @Test
  public void test_neutralize_every_spelling_of_the_closing_tag() {
    // the HTML parser ends the tag name at a space, a line break, a slash or ">", in any case
    List.of("</template>", "</TEMPLATE>", "</Template >", "</template\n>", "</template\t>", "</template/>",
        "</template foo=bar>", "</template")
      .forEach(tag -> assertThat(TemplateSafeMethod.neutralize("a" + tag + PAYLOAD)).as(tag)
        .doesNotContainIgnoringCase("</template"));
  }

  @Test
  public void test_neutralize_every_occurrence() {
    assertThat(TemplateSafeMethod.neutralize("</template></template></TEMPLATE>")).doesNotContainIgnoringCase("</template");
  }

  @Test
  public void test_neutralize_keeps_markdown_as_typed() {
    assertThat(TemplateSafeMethod.neutralize(MARKDOWN)).isEqualTo(MARKDOWN);
    // an opening tag does not end the block
    assertThat(TemplateSafeMethod.neutralize("<template>x")).isEqualTo("<template>x");
  }

  @Test
  public void test_neutralize_null() {
    assertThat(TemplateSafeMethod.neutralize(null)).isNull();
  }

  @Test
  public void test_exec_not_a_string() throws TemplateModelException {
    SimpleNumber number = new SimpleNumber(42);
    assertThat(new TemplateSafeMethod().exec(List.of(number))).isSameAs(number);
  }

  @Test
  public void test_exec_wrong_arguments() {
    assertThatThrownBy(() -> new TemplateSafeMethod().exec(List.of())).isInstanceOf(TemplateModelException.class);
    assertThatThrownBy(() -> new TemplateSafeMethod().exec(List.of(new SimpleScalar("a"), new SimpleScalar("b"))))
      .isInstanceOf(TemplateModelException.class);
  }

  @Test
  public void test_template_output_is_the_same_with_and_without_auto_escaping() throws Exception {
    for (boolean autoEscaping : new boolean[] { false, true }) {
      String as = "auto-escaping " + autoEscaping;
      // the value as typed, only the closing tag is changed: no second escaping when the templates are auto-escaped
      assertThat(render("<template>${templateSafe(value)}</template>", MARKDOWN + "</Template>" + PAYLOAD, autoEscaping))
        .as(as).isEqualTo("<template>" + MARKDOWN + "&lt;/template>" + PAYLOAD + "</template>");
      // after other built-ins, as in the lists
      assertThat(render("${templateSafe(value?trim?truncate_w(8, \"...\"))}", " a </template> b c d e f ", autoEscaping))
        .as(as).isEqualTo("a ...");
      // not a string
      assertThat(render("${templateSafe(number)}", "", autoEscaping)).as(as).isEqualTo("42");
      // a missing value (e.g. a field only in one of two data access request submissions)
      assertThat(render("[${templateSafe(values[1])}]", "", autoEscaping)).as(as).isEqualTo("[]");
    }
  }

  @Test
  public void test_template_markup_argument() throws Exception {
    // in an auto-escaped template the value can be given with ?no_esc
    assertThat(render("${templateSafe(value?no_esc)}", "<b>x</b></template>" + PAYLOAD, true))
      .isEqualTo("<b>x</b>&lt;/template>" + PAYLOAD);
  }

  private String render(String source, String value, boolean autoEscaping) throws Exception {
    Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
    if (autoEscaping) configuration.setOutputFormat(HTMLOutputFormat.INSTANCE);
    configuration.setSharedVariable(TemplateSafeMethod.NAME, new TemplateSafeMethod());
    List<Object> values = new ArrayList<>();
    values.add("label");
    values.add(null);
    Map<String, Object> model = new HashMap<>();
    model.put("value", value);
    model.put("number", 42);
    model.put("values", values);
    StringWriter writer = new StringWriter();
    new Template("test", source, configuration).process(model, writer);
    return writer.toString();
  }
}
