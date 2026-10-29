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
import java.util.List;
import java.util.Map;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.esapi.ESAPI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HtmlDecodeMethodTest {

  // text typed by a user, with the characters that are encoded
  private static final String TYPED = "<b>&\"'é’ x@y =1+1";

  // list name as stored since Mica 6.0 (ESAPI encodeForHTML in AbstractPublishedDocumentsSetsResource)
  private static final String STORED_ENCODED = "&lt;b&gt;&amp;&quot;&#x27;&eacute;&rsquo; x&#x40;y &#x3d;1&#x2b;1";

  // list names typed by users and stored raw before Mica 6.0
  private static final List<String> STORED_RAW = List.of(
    "Tom & Jerry",
    "R&D variables",
    "R&copy2",
    "a < b > c",
    "50% & more",
    "x@y =1+1",
    "l'été “2015”",
    "plain");

  // a list name can contain the payload of the other cases
  private static final List<String> TYPED_NAMES = List.of(
    TYPED,
    "Tom & Jerry's \"list\" <b>é</b>",
    "<script>alert('x')</script>",
    "<img src=x onerror=alert(1)>",
    "Text with &amp; typed",
    "emoji 😀 and 中文",
    "line\nbreak");

  private Configuration configuration;

  private StringTemplateLoader templates;

  @BeforeEach
  public void init() {
    templates = new StringTemplateLoader();
    configuration = new Configuration(Configuration.VERSION_2_3_32);
    configuration.setTemplateLoader(templates);
    configuration.setSharedVariable(HtmlDecodeMethod.NAME, new HtmlDecodeMethod());
    // same function as in libs/list.ftl
    templates.putTemplate("list.ftl", """
      <#function listName set>
        <#if set.name?starts_with("dar:")>
          <#local name = set.name?replace("dar:", "")/>
        <#else>
          <#local name = set.name/>
        </#if>
        <#return name/>
      </#function>""");
  }

  @Test
  public void test_esapi_fixture_is_what_mica_stores() {
    assertThat(ESAPI.encoder().encodeForHTML(TYPED)).isEqualTo(STORED_ENCODED);
  }

  @Test
  public void test_decode_encoded_name() {
    assertThat(HtmlDecodeMethod.decode(STORED_ENCODED)).isEqualTo(TYPED);
  }

  @Test
  public void test_decode_esapi_encoded_names() {
    TYPED_NAMES.forEach(name ->
      assertThat(HtmlDecodeMethod.decode(ESAPI.encoder().encodeForHTML(name))).as(name).isEqualTo(name));
  }

  @Test
  public void test_decode_raw_names_unchanged() {
    STORED_RAW.forEach(name -> assertThat(HtmlDecodeMethod.decode(name)).as(name).isEqualTo(name));
  }

  @Test
  public void test_decode_set_operation_names() {
    assertThat(HtmlDecodeMethod.decode("S1 &Union; S2 &Union; S3")).isEqualTo("S1 ⋃ S2 ⋃ S3");
    assertThat(HtmlDecodeMethod.decode("S1 &Intersection; S2")).isEqualTo("S1 ⋂ S2");
    assertThat(HtmlDecodeMethod.decode("S1 - S2")).isEqualTo("S1 - S2");
  }

  @Test
  public void test_decode_numeric_entities() {
    assertThat(HtmlDecodeMethod.decode("&#39;&#x27;&#X27;&#x1f600;&#xa;")).isEqualTo("'''😀\n");
  }

  @Test
  public void test_decode_null() {
    assertThat(HtmlDecodeMethod.decode(null)).isNull();
  }

  @Test
  public void test_exec_string() throws TemplateModelException {
    assertThat(new HtmlDecodeMethod().exec(List.of(new SimpleScalar(STORED_ENCODED)))).isEqualTo(TYPED);
  }

  @Test
  public void test_exec_not_a_string() throws TemplateModelException {
    SimpleNumber number = new SimpleNumber(42);
    assertThat(new HtmlDecodeMethod().exec(List.of(number))).isSameAs(number);
  }

  @Test
  public void test_exec_wrong_arguments() {
    assertThatThrownBy(() -> new HtmlDecodeMethod().exec(List.of())).isInstanceOf(TemplateModelException.class);
    assertThatThrownBy(() -> new HtmlDecodeMethod().exec(List.of(new SimpleScalar("a"), new SimpleScalar("b"))))
      .isInstanceOf(TemplateModelException.class);
  }

  @Test
  public void test_render_encoded_name_escaped_once() throws Exception {
    assertThat(render("${htmlDecode(name)?html}", "&lt;b&gt;Tom &amp; Jerry")).isEqualTo("&lt;b&gt;Tom &amp; Jerry");
    TYPED_NAMES.forEach(name -> assertEscapedOnce(renderUnchecked("${htmlDecode(name)?html}", ESAPI.encoder().encodeForHTML(name)), name));
  }

  @Test
  public void test_render_raw_name_escaped_once() throws Exception {
    assertThat(render("${htmlDecode(name)?html}", "<b>Tom & Jerry")).isEqualTo("&lt;b&gt;Tom &amp; Jerry");
    STORED_RAW.forEach(name -> assertEscapedOnce(renderUnchecked("${htmlDecode(name)?html}", name), name));
    TYPED_NAMES.stream().filter(name -> !name.contains("&amp;"))
      .forEach(name -> assertEscapedOnce(renderUnchecked("${htmlDecode(name)?html}", name), name));
  }

  @Test
  public void test_render_set_operation_name() throws Exception {
    assertThat(render("${htmlDecode(name)?html}", "S1 &Union; S2")).isEqualTo("S1 ⋃ S2");
  }

  @Test
  public void test_render_list_name() throws Exception {
    String template = "<#include \"list.ftl\">${htmlDecode(listName(set))?html}";
    assertThat(render(template, Map.of("set", Map.of("name", STORED_ENCODED)))).isEqualTo(escapeOnce(TYPED));
    assertThat(render(template, Map.of("set", Map.of("name", "Tom & Jerry")))).isEqualTo("Tom &amp; Jerry");
    assertThat(render(template, Map.of("set", Map.of("name", "dar:1234-A1")))).isEqualTo("1234-A1");
  }

  @Test
  public void test_render_list_name_truncated() throws Exception {
    // decoding first, an entity is never cut
    // as in list.ftl
    String template = "<#include \"list.ftl\">${htmlDecode(listName(set))?truncate_c(20, \"...\")?html}";
    String rendered = render(template, Map.of("set", Map.of("name", "Tom &amp; Jerry &amp; Friends &amp; Co")));
    assertThat(rendered).startsWith("Tom &amp; Jerry &amp; ").endsWith("...").doesNotContain("&amp;amp;");
  }

  @Test
  public void test_render_js_string_in_attribute() throws Exception {
    // as in cart.ftl onclick="onClickAddToSet('id', '...')"
    assertThat(render("'${htmlDecode(name)?js_string?html}'", "Tom &amp; Jerry&#x27;s")).isEqualTo("'Tom &amp; Jerry\\&#39;s'");
  }

  @Test
  public void test_render_without_decode_escapes_twice() throws Exception {
    // the 7.0.0 regression: an encoded name escaped again
    assertThat(render("${name?html}", "Tom &amp; Jerry")).isEqualTo("Tom &amp;amp; Jerry");
  }

  //
  // Private methods
  //

  /**
   * Escaped exactly once: no markup and decoding once gives the typed text back.
   */
  private void assertEscapedOnce(String rendered, String typed) {
    assertThat(rendered).as(typed).doesNotContain("<").doesNotContain(">").doesNotContain("\"");
    assertThat(HtmlDecodeMethod.decode(rendered)).as(typed).isEqualTo(typed);
  }

  private String escapeOnce(String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
  }

  private String render(String template, String name) throws Exception {
    return render(template, Map.of("name", name));
  }

  private String renderUnchecked(String template, String name) {
    try {
      return render(template, name);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String render(String template, Map<String, Object> model) throws Exception {
    String templateName = "test-" + Integer.toHexString(template.hashCode()) + ".ftl";
    templates.putTemplate(templateName, template);
    StringWriter writer = new StringWriter();
    configuration.getTemplate(templateName).process(model, writer);
    return writer.toString().trim();
  }
}
