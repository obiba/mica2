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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import org.junit.jupiter.api.Test;
import org.obiba.mica.core.support.HtmlDecodeMethod;
import org.obiba.mica.core.support.TemplateSafeMethod;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders every built-in page with a hostile model and reports where a model value reaches the output unescaped.
 * <p>
 * Every model variable is a stub that fits any expression (hash, one-item list, method, true, string). Its string
 * value is a marker <code>ZX{id}&lt;b&gt;'"&amp;ZY</code>; the id gives back the model path and the template that read
 * it. Each marker found in the output is classified by context (HTML text/attribute, script, template block) and by
 * how it was encoded. A second run uses <code>javascript:</code> values to find links that do not go through safeUrl().
 * <p>
 * The report and the rendered pages are written to target/template-render. Rendering errors are recorded and the
 * rendering goes on, as with the ignore exception handler of the application: an error means a blank part of a page.
 * The test fails when a template cannot be included, imported or parsed (that part of the pages would be blank), and
 * when a value is printed unescaped (injection, breakout, apostrophe), except the raw ones on purpose,
 * or inside a Vue app mounted on the page without v-pre (vue): HTML escaping keeps the braces of a {{ }} expression.
 */
public class TemplatesRenderTest {

  private static final Path OUTPUT = Path.of("target", "template-render");

  private static final String PAYLOAD = "<b>'\"&";

  private static final Pattern MARKER = Pattern.compile("ZX(\\d+)(.*?)ZY", Pattern.DOTALL);

  private static final Pattern SCRIPT_LINK = Pattern.compile(
    "\\b(href|src|action|formaction)\\s*=\\s*[\"']?\\s*javascript:ZX(\\d+)ZY", Pattern.CASE_INSENSITIVE);

  private static final Pattern SCRIPT_BLOCK = Pattern.compile("<script\\b([^>]*)>(.*?)</script\\s*>",
    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  private static final Pattern SCRIPT_TYPE = Pattern.compile("\\btype\\s*=\\s*[\"']([^\"']*)[\"']",
    Pattern.CASE_INSENSITIVE);

  // a plain template block, holding Markdown sources; Vue ones have attributes (v-if, v-else...)
  private static final Pattern TEMPLATE_BLOCK = Pattern.compile("<template>(.*?)</template>", Pattern.DOTALL);

  // Vue apps mounted on the server-rendered HTML: Vue compiles their content, a {{ }} in a printed value would run
  private static final Pattern VUE_MOUNT = Pattern.compile(
    "<(\\w+)\\b[^>]*\\sid=\"(query-vue-container|search-application|summary-statistics|cards|data-access-form|[\\w-]*files-app)\"[^>]*>");

  // elements Vue does not compile, their own attributes included
  private static final Pattern V_PRE = Pattern.compile("<(\\w+)\\b[^>]*\\sv-pre\\b[^>]*>");

  // the value is in a Vue attribute (v-bind, :prop, @event...) of the tag being written
  private static final Pattern VUE_ATTRIBUTE = Pattern.compile("\\s(v-[\\w:.-]+|:[\\w.-]+|@[\\w.:-]+)\\s*=\\s*\"[^\"]*$");

  // every permission, so that the parts of the data access pages they show are rendered
  private static final List<String> PERMISSIONS = List.of("ADD_COLLABORATORS", "ARCHIVE", "DELETE",
    "DELETE_COLLABORATORS", "EDIT", "EDIT_STATUS", "UNARCHIVE", "VIEW_PRIVATE_COMMENTS");

  // model values with a real value, as the pages compute on them
  private static final Map<String, Object> OVERRIDES = Map.of(
    "contextPath", "",
    "config.contextPath", "",
    // otherwise the studies and networks carts are disabled
    "config.singleStudyEnabled", false,
    "config.singleNetworkEnabled", false,
    "user.roles", List.of("mica-administrator", "mica-reviewer", "mica-editor", "mica-data-access-officer"),
    "permissions", PERMISSIONS,
    "agreementPermissions", PERMISSIONS,
    "amendmentPermissions", PERMISSIONS,
    "feasibilityPermissions", PERMISSIONS,
    "preliminaryPermissions", PERMISSIONS);

  // helper templates: a value read there is reported where its parent was read (e.g. localize(study.name))
  private static final List<String> HELPERS = List.of("libs/head.ftl");

  // errors of a template that cannot be included, imported or parsed
  private static final Pattern BROKEN_TEMPLATE = Pattern.compile(
    "Template inclusion failed|Template importing failed|Syntax error|ParseException", Pattern.CASE_INSENSITIVE);

  private static final List<String> UNSAFE = List.of("injection", "breakout", "apostrophe", "vue");

  // values printed raw on purpose ("template | path"): JSON built by the controllers, values HTML-encoded by the
  // controllers
  private static final List<String> RAW_ON_PURPOSE = List.of(
    "libs/search-scripts.ftl | configJson |",
    "libs/entities-list-scripts.ftl | configJson |",
    "libs/document-set-scripts.ftl | configJson |",
    "libs/data-access-form-scripts.ftl | formConfig.schema |",
    "libs/data-access-form-scripts.ftl | formConfig.definition |",
    "libs/data-access-form-scripts.ftl | formConfig.model |",
    "error.ftl | msg |",
    "error.ftl | status |",
    // numbers, they cannot hold the braces of a {{ }} expression
    "cart.ftl | sets.variablesCart.count |",
    "cart.ftl | sets.studiesCart.count |",
    "cart.ftl | sets.networksCart.count |");

  // the translations shipped with Mica, see i18n/messages_{language}.properties
  private static final List<String> LANGUAGES = List.of("en", "fr");

  // a language whose translations are TQ{code}|{payload}TQ, to find where translations are printed without the
  // encoding of their context
  private static final String SYNTHETIC = "synthetic";

  private static final Pattern TRANSLATION = Pattern.compile("TQ([^|<>\\s]+)\\|(.*?)TQ", Pattern.DOTALL);

  // a call of the message macro, its argument can be a quoted code
  private static final Pattern MESSAGE_MACRO = Pattern.compile("<@message\\s+(\"[^\"]*\"|'[^']*'|[^/>\"'])*/>");

  // an attribute whose value is a script (event handler, Vue binding or directive) of the tag being written
  private static final Pattern SCRIPT_ATTRIBUTE = Pattern.compile(
    "\\s(on\\w+|v-[\\w:.-]+|:[\\w.-]+|@[\\w.:-]+)\\s*=\\s*\"[^\"]*$", Pattern.CASE_INSENSITIVE);

  // stops recursive macros (trees): lists are empty from this path depth
  private static final int MAX_DEPTH = 6;

  enum Context {HTML, SCRIPT, TEMPLATE, VUE}

  enum Encoding {RAW, HTML, HTML_APOS_RAW, JS, JS_HTML, DOUBLE_HTML, OTHER}

  record Origin(String path, String template) {}

  record Finding(String template, String path, Context context, Encoding encoding) {}

  record PageResult(String page, String output, int markers, List<Finding> findings, List<String> errors) {}

  @Test
  public void test_render_every_page_with_a_hostile_model() throws Exception {
    Configuration configuration = newConfiguration();
    List<String> pages = listPages();
    Files.createDirectories(OUTPUT);

    List<PageResult> results = new ArrayList<>();
    List<String> scriptLinks = new ArrayList<>();
    for (String page : pages) {
      PageResult result = render(configuration, page, false);
      results.add(result);
      Files.writeString(OUTPUT.resolve(page.replace(".ftl", ".html")), result.output(), StandardCharsets.UTF_8);
      scriptLinks.addAll(renderScriptLinks(configuration, page));
    }
    writeReport(results, scriptLinks);

    // the harness works: every page renders and the stub values reach the output
    assertThat(pages.size()).isGreaterThan(35);
    assertThat(results).allSatisfy(result -> assertThat(result.output()).as(result.page()).isNotBlank());
    assertThat(results.stream().mapToInt(PageResult::markers).sum()).isGreaterThan(100);

    // a template that cannot be included or imported (syntax error, e.g. ?html, or missing) blanks that part of every
    // page using it; the other errors come from the stub values (comparisons, numbers) and are expected
    List<String> brokenTemplates = results.stream()
      .flatMap(result -> result.errors().stream().map(error -> result.page() + ": " + error))
      .filter(error -> BROKEN_TEMPLATE.matcher(error).find())
      .distinct()
      .toList();
    assertThat(brokenTemplates).as("templates that cannot be included or parsed, see target/template-render/report.md")
      .isEmpty();

    // no value reaches the output unescaped, except the raw ones on purpose
    List<String> unsafe = results.stream()
      .flatMap(result -> result.findings().stream())
      .filter(finding -> UNSAFE.contains(severity(finding)))
      .map(finding -> finding.template() + " | " + finding.path() + " | " + finding.context() + " | " + severity(finding))
      .filter(finding -> RAW_ON_PURPOSE.stream().noneMatch(finding::startsWith))
      .distinct()
      .sorted()
      .toList();
    assertThat(unsafe).as("values printed unescaped, see target/template-render/report.md").isEmpty();
  }

  /**
   * Renders every built-in page in each language and reports the translations that end the quoted string they are
   * written in (e.g. French L'invitation in '...'): printed raw, or HTML-escaped in a script attribute, where the
   * browser decodes the entities before the script runs.
   */
  @Test
  public void test_render_every_page_with_each_translation() throws Exception {
    Configuration configuration = newConfiguration();
    List<String> pages = listPages();
    List<String> broken = new ArrayList<>();
    for (String language : LANGUAGES) {
      Map<String, String> quoted = quotedTranslations(language);
      // the harness works: there are translations with a quote to look for
      assertThat(quoted).as(language).isNotEmpty();
      Files.createDirectories(OUTPUT.resolve(language));
      for (String page : pages) {
        String output = render(configuration, page, false, language).output();
        Files.writeString(OUTPUT.resolve(language).resolve(page.replace(".ftl", ".html")), output, StandardCharsets.UTF_8);
        broken.addAll(findBrokenTranslations(language, page, output, quoted));
      }
    }
    assertThat(broken.stream().distinct().sorted().toList())
      .as("translations that break the quoted string they are written in, use messageJs(code)")
      .isEmpty();
  }

  /**
   * Renders every built-in page with translations holding HTML and quotes, and reports the ones printed without the
   * encoding of their context: in a script, they must be JavaScript-escaped (messageJs), in an attribute
   * HTML-escaped (messageAttr), or both in a script attribute or a Vue {{ }} expression (messageJs). In HTML text
   * they are printed as is, as translations can hold HTML.
   */
  @Test
  public void test_render_every_page_with_quoted_translations() throws Exception {
    Configuration configuration = newConfiguration();
    List<String> unescaped = new ArrayList<>();
    int[] checked = new int[1];
    for (String page : listPages()) {
      String output = render(configuration, page, false, SYNTHETIC).output();
      unescaped.addAll(findUnescapedTranslations(page, output, checked));
    }
    List<String> report = unescaped.stream().distinct().sorted().toList();
    Files.createDirectories(OUTPUT);
    Files.write(OUTPUT.resolve("quoted-translations.txt"), report, StandardCharsets.UTF_8);
    // the harness works: translations are found in scripts and attributes
    assertThat(checked[0]).isGreaterThan(200);
    assertThat(report)
      .as("translations printed without the encoding of their context (code | page | context | encoding), see "
        + "target/template-render/quoted-translations.txt")
      .isEmpty();
  }

  /**
   * Reads every built-in template, the parts not rendered with the stubs included (else branches, negated settings):
   * a translation printed with the message macro is raw, it must not be in a script, an attribute or a Vue {{ }}
   * expression, where messageJs() or messageAttr() encode it.
   */
  @Test
  public void test_no_raw_translation_in_scripts_or_attributes() throws Exception {
    Path root = Path.of(getClass().getResource("/_templates/libs/head.ftl").toURI()).getParent().getParent();
    List<String> raw = new ArrayList<>();
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".ftl")).sorted().toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        String html = blankFreemarker(source);
        List<int[]> scripts = ranges(SCRIPT_BLOCK, html, true);
        Matcher message = MESSAGE_MACRO.matcher(source);
        while (message.find()) {
          int at = message.start();
          String attribute = attributeName(html, at);
          String context = inside(scripts, at) ? "script"
            : attribute != null ? "attribute " + attribute
            : html.lastIndexOf("{{", at) > html.lastIndexOf("}}", at) ? "vue {{ }}" : null;
          if (context != null) {
            int line = (int) source.substring(0, at).chars().filter(c -> c == '\n').count() + 1;
            raw.add(root.relativize(file) + ":" + line + " | " + context + " | " + message.group());
          }
        }
      }
    }
    assertThat(raw).as("translations printed raw in a script, an attribute or {{ }}: use messageJs() or messageAttr()")
      .isEmpty();
  }

  // FreeMarker comments, directives, macro calls and interpolations blanked, positions kept: the HTML around; a > in a
  // string or in parentheses does not end a directive
  private static String blankFreemarker(String source) {
    String html = source;
    for (Pattern pattern : List.of(Pattern.compile("<#--.*?-->", Pattern.DOTALL),
      Pattern.compile("</?[#@](?:\"[^\"]*\"|'[^']*'|\\([^)]*\\)|[^>\"'(])*>"),
      Pattern.compile("\\$\\{[^}]*}"))) {
      html = pattern.matcher(html).replaceAll(match -> " ".repeat(match.group().length()));
    }
    return html;
  }

  //
  // Rendering
  //

  private Configuration newConfiguration() throws Exception {
    // same defaults as Spring's FreeMarkerConfigurationFactory
    Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    TemplateLoader templates = new ClassTemplateLoader(getClass(), "/_templates/");
    // spring.ftl, as added by FreeMarkerConfigurer
    TemplateLoader spring = new ClassTemplateLoader(FreeMarkerConfigurer.class, "");
    configuration.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[]{templates, spring}));
    configuration.setDefaultEncoding("UTF-8");
    configuration.setLocale(Locale.ENGLISH);
    for (Map.Entry<String, String> setting : applicationSettings().entrySet()) {
      configuration.setSetting(setting.getKey(), setting.getValue());
    }
    // errors are recorded per page, then ignored as in the application
    configuration.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
    configuration.setLogTemplateExceptions(false);
    configuration.setSharedVariable(HtmlDecodeMethod.NAME, new HtmlDecodeMethod());
    configuration.setSharedVariable(TemplateSafeMethod.NAME, new TemplateSafeMethod());
    return configuration;
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> applicationSettings() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/config/application.yml")) {
      Map<String, Object> yaml = new Yaml().load(input);
      Map<String, Object> spring = (Map<String, Object>) yaml.get("spring");
      Map<String, Object> freemarker = (Map<String, Object>) spring.get("freemarker");
      Map<String, Object> settings = (Map<String, Object>) freemarker.getOrDefault("settings", Map.of());
      Map<String, String> values = new LinkedHashMap<>();
      settings.forEach((key, value) -> values.put(key, String.valueOf(value)));
      return values;
    }
  }

  private List<String> listPages() throws IOException, URISyntaxException {
    Path root = Path.of(getClass().getResource("/_templates/libs/head.ftl").toURI()).getParent().getParent();
    try (Stream<Path> files = Files.list(root)) {
      return files.filter(Files::isRegularFile)
        .map(path -> path.getFileName().toString())
        .filter(name -> name.endsWith(".ftl"))
        .sorted()
        .toList();
    }
  }

  private PageResult render(Configuration configuration, String page, boolean scriptLinks) throws Exception {
    return render(configuration, page, scriptLinks, "en");
  }

  private PageResult render(Configuration configuration, String page, boolean scriptLinks, String language)
    throws Exception {
    Stubs stubs = new Stubs(configuration.getObjectWrapper(), scriptLinks, language);
    List<String> errors = new ArrayList<>();
    StringWriter writer = new StringWriter();
    Template template = configuration.getTemplate(page);
    Environment environment = template.createProcessingEnvironment(stubs.root(), writer);
    environment.setTemplateExceptionHandler((exception, env, out) -> errors.add(describe(exception)));
    environment.process();
    String output = writer.toString();
    if (scriptLinks) {
      return new PageResult(page, output, 0, findScriptLinks(page, output, stubs), errors);
    }
    return analyze(page, output, stubs, errors);
  }

  private List<String> renderScriptLinks(Configuration configuration, String page) throws Exception {
    return render(configuration, page, true).findings().stream()
      .map(finding -> finding.template() + " | `" + finding.path() + "`")
      .toList();
  }

  private static String describe(TemplateException exception) {
    String message = exception.getMessageWithoutStackTop().strip().lines().findFirst().orElse("");
    return exception.getTemplateSourceName() + ":" + exception.getLineNumber() + " " + message;
  }

  //
  // Analysis
  //

  private PageResult analyze(String page, String output, Stubs stubs, List<String> errors) {
    List<int[]> scripts = ranges(SCRIPT_BLOCK, output, true);
    List<int[]> templates = ranges(TEMPLATE_BLOCK, output, false);
    List<int[]> vueApps = elements(VUE_MOUNT, output, false);
    List<int[]> vuePre = elements(V_PRE, output, true);
    List<Finding> findings = new ArrayList<>();
    int markers = 0;
    Matcher marker = MARKER.matcher(output);
    while (marker.find()) {
      markers++;
      Origin origin = stubs.origin(Integer.parseInt(marker.group(1)));
      Context context = inside(templates, marker.start()) ? Context.TEMPLATE
        : inside(scripts, marker.start()) ? Context.SCRIPT : Context.HTML;
      Encoding encoding = encoding(marker.group(2));
      if (isFinding(context, encoding)) {
        findings.add(new Finding(origin.template(), origin.path(), context, encoding));
      }
      // HTML escaping keeps the braces: in a Vue app, the value needs an element with v-pre
      if (context != Context.SCRIPT && inside(vueApps, marker.start()) && !inside(vuePre, marker.start())
        && isCompiledByVue(output, marker.start())) {
        findings.add(new Finding(origin.template(), origin.path(), Context.VUE, encoding));
      }
    }
    return new PageResult(page, output, markers, findings, errors);
  }

  /**
   * The ranges of the elements whose opening tag matches the pattern (group 1 is the tag name), up to their closing
   * tag; with the opening tag itself or only the content.
   */
  private static List<int[]> elements(Pattern openingTag, String output, boolean withOpeningTag) {
    List<int[]> ranges = new ArrayList<>();
    Matcher matcher = openingTag.matcher(output);
    while (matcher.find()) {
      if (matcher.group().endsWith("/>")) continue;
      Matcher tag = Pattern.compile("<(/?)" + Pattern.quote(matcher.group(1)) + "\\b[^>]*>", Pattern.CASE_INSENSITIVE)
        .matcher(output);
      int depth = 1;
      int end = output.length();
      int from = matcher.end();
      while (depth > 0 && tag.find(from)) {
        depth += tag.group(1).isEmpty() ? 1 : -1;
        if (depth == 0) end = tag.start();
        from = tag.end();
      }
      ranges.add(new int[]{withOpeningTag ? matcher.start() : matcher.end(), end});
    }
    return ranges;
  }

  // text, or the value of a Vue attribute; Vue does not read the other attributes
  private static boolean isCompiledByVue(String output, int position) {
    int tagStart = output.lastIndexOf('<', position);
    if (tagStart < output.lastIndexOf('>', position)) return true;
    return VUE_ATTRIBUTE.matcher(output.substring(tagStart, position)).find();
  }

  private List<Finding> findScriptLinks(String page, String output, Stubs stubs) {
    List<Finding> findings = new ArrayList<>();
    Matcher link = SCRIPT_LINK.matcher(output);
    while (link.find()) {
      Origin origin = stubs.origin(Integer.parseInt(link.group(2)));
      findings.add(new Finding(origin.template(), origin.path() + " (" + link.group(1) + ")", Context.HTML,
        Encoding.RAW));
    }
    return findings;
  }

  // script blocks with a JavaScript type, the content of other types (x-template, text/html) is HTML
  private static List<int[]> ranges(Pattern block, String output, boolean javascriptOnly) {
    List<int[]> ranges = new ArrayList<>();
    Matcher matcher = block.matcher(output);
    while (matcher.find()) {
      if (javascriptOnly && !isJavascript(matcher.group(1))) continue;
      // the block content: an attribute of the opening tag (e.g. script src) is in HTML context
      int content = matcher.groupCount();
      ranges.add(new int[]{matcher.start(content), matcher.end(content)});
    }
    return ranges;
  }

  private static boolean isJavascript(String attributes) {
    Matcher type = SCRIPT_TYPE.matcher(attributes);
    if (!type.find()) return true;
    String value = type.group(1).toLowerCase(Locale.ROOT);
    return value.isEmpty() || value.contains("javascript") || value.equals("module") || value.contains("json");
  }

  private static boolean inside(List<int[]> ranges, int position) {
    return ranges.stream().anyMatch(range -> range[0] <= position && position < range[1]);
  }

  private static Encoding encoding(String payload) {
    return switch (payload) {
      case PAYLOAD -> Encoding.RAW;
      case "&lt;b&gt;&#39;&quot;&amp;" -> Encoding.HTML;
      case "&lt;b&gt;'&quot;&amp;" -> Encoding.HTML_APOS_RAW;
      case "<b>\\'\\\"&" -> Encoding.JS;
      case "&lt;b&gt;\\&#39;\\&quot;&amp;", "&lt;b&gt;\\'\\&quot;&amp;" -> Encoding.JS_HTML;
      case "&amp;lt;b&amp;gt;&amp;#39;&amp;quot;&amp;amp;", "&amp;lt;b&amp;gt;'&amp;quot;&amp;amp;" -> Encoding.DOUBLE_HTML;
      default -> Encoding.OTHER;
    };
  }

  private static boolean isFinding(Context context, Encoding encoding) {
    return switch (context) {
      // Markdown source, sanitized when rendered in the browser; templateSafe() is checked by TemplatesMarkdownBlockTest
      case TEMPLATE -> false;
      case HTML -> encoding != Encoding.HTML;
      case SCRIPT -> encoding != Encoding.JS;
      case VUE -> true;
    };
  }

  private static String severity(Finding finding) {
    if (finding.context() == Context.VUE) return "vue";
    if (finding.encoding() == Encoding.RAW || finding.context() == Context.HTML && finding.encoding() == Encoding.JS) {
      return finding.context() == Context.SCRIPT ? "breakout" : "injection";
    }
    return switch (finding.encoding()) {
      case HTML_APOS_RAW -> "apostrophe";
      case OTHER -> "check";
      default -> "display";
    };
  }

  //
  // Translations
  //

  /**
   * The translations as the application finds them (ExtendedResourceBundleMessageSource): the messages bundle first,
   * then the JSON translations (i18n/{language}.json, the defaults of the translations in the Mica settings).
   */
  private static Properties translations(String language) throws IOException {
    Properties messages = new Properties();
    String bundle = SYNTHETIC.equals(language) ? "en" : language;
    try (InputStream input = TemplatesRenderTest.class.getResourceAsStream("/i18n/messages_" + bundle + ".properties")) {
      messages.load(new InputStreamReader(input, StandardCharsets.UTF_8));
    }
    try (InputStream input = TemplatesRenderTest.class.getResourceAsStream("/i18n/" + bundle + ".json")) {
      flatten("", new ObjectMapper().readTree(input), messages);
    }
    if (SYNTHETIC.equals(language)) {
      messages.replaceAll((code, text) -> "TQ" + code + "|" + PAYLOAD + "TQ");
    }
    return messages;
  }

  private static List<String> findUnescapedTranslations(String page, String output, int[] checked) {
    List<int[]> scripts = ranges(SCRIPT_BLOCK, output, true);
    List<int[]> templates = ranges(TEMPLATE_BLOCK, output, false);
    List<String> unescaped = new ArrayList<>();
    Matcher translation = TRANSLATION.matcher(output);
    while (translation.find()) {
      int at = translation.start();
      if (inside(templates, at)) continue;
      Encoding encoding = encoding(translation.group(2));
      String context;
      List<Encoding> expected;
      String attribute = inside(scripts, at) ? null : attributeName(output, at);
      if (inside(scripts, at)) {
        context = "script";
        expected = List.of(Encoding.JS);
      } else if (attribute != null) {
        boolean script = attribute.matches("(?i)on\\w+|v-.*|:.*|@.*");
        context = "attribute " + attribute;
        expected = List.of(script ? Encoding.JS_HTML : Encoding.HTML);
      } else if (output.lastIndexOf("{{", at) > output.lastIndexOf("}}", at)) {
        context = "vue {{ }}";
        expected = List.of(Encoding.JS_HTML);
      } else {
        continue;
      }
      checked[0]++;
      if (!expected.contains(encoding)) {
        unescaped.add(translation.group(1) + " | " + page + " | " + context + " | " + encoding);
      }
    }
    return unescaped;
  }

  /**
   * The name of the attribute whose value holds the position, null when it is not in a tag: the tag is read from its
   * start, quotes followed, as attribute values can hold a &gt;.
   */
  private static String attributeName(String output, int position) {
    Matcher tagStart = Pattern.compile("<[a-zA-Z]").matcher(output);
    int start = -1;
    int from = Math.max(0, output.lastIndexOf('<', position) - 2000);
    while (tagStart.find(from) && tagStart.start() < position) {
      start = tagStart.start();
      from = start + 1;
    }
    if (start < 0) return null;
    char quote = 0;
    int valueStart = -1;
    for (int i = start; i < position; i++) {
      char c = output.charAt(i);
      if (quote != 0) {
        if (c == quote) quote = 0;
      } else if (c == '"' || c == '\'') {
        quote = c;
        valueStart = i;
      } else if (c == '>') {
        return null;
      }
    }
    if (quote == 0) return null;
    Matcher name = Pattern.compile("([^\\s=<>\"']+)\\s*=\\s*$").matcher(output.substring(start, valueStart));
    return name.find() ? name.group(1) : "?";
  }

  // the texts of a JSON translation tree, by their dotted path; the bundle comes first
  private static void flatten(String prefix, JsonNode node, Properties messages) {
    if (node.isObject()) {
      node.properties().forEach(field -> flatten(prefix + field.getKey() + ".", field.getValue(), messages));
    } else if (node.isValueNode()) {
      messages.putIfAbsent(prefix.substring(0, prefix.length() - 1), node.asText());
    }
  }

  // the translations with a quote, as printed without arguments (with arguments, MessageFormat changes the quotes)
  private static Map<String, String> quotedTranslations(String language) throws IOException {
    Map<String, String> quoted = new TreeMap<>();
    translations(language).forEach((code, text) -> {
      String value = text.toString();
      if (value.matches("(?s).*['\"`].*") && !value.contains("{")) quoted.put(code.toString(), value);
    });
    return quoted;
  }

  private static List<String> findBrokenTranslations(String language, String page, String output,
                                                     Map<String, String> translations) {
    List<String> broken = new ArrayList<>();
    translations.forEach((code, text) -> {
      String escaped = escapeHtml(text);
      for (String printed : List.of(text, escaped)) {
        int at = output.indexOf(printed, 1);
        while (at >= 0) {
          char quote = output.charAt(at - 1);
          boolean inString = (quote == '\'' || quote == '"' || quote == '`') && text.indexOf(quote) >= 0;
          if (inString && (printed.equals(text) || isScriptAttribute(output, at))) {
            String around = output.substring(Math.max(0, at - 60), Math.min(output.length(), at + printed.length() + 10));
            broken.add(language + " | " + code + " | " + page + " | " + around.replaceAll("\\s+", " ").strip());
          }
          at = output.indexOf(printed, at + 1);
        }
      }
    });
    return broken;
  }

  private static boolean isScriptAttribute(String output, int position) {
    int tagStart = output.lastIndexOf('<', position);
    if (tagStart < 0 || tagStart < output.lastIndexOf('>', position)) return false;
    return SCRIPT_ATTRIBUTE.matcher(output.substring(tagStart, position)).find();
  }

  // as FreeMarker's HTML output format
  private static String escapeHtml(String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
      .replace("'", "&#39;");
  }

  //
  // Report
  //

  private void writeReport(List<PageResult> results, List<String> scriptLinks) throws IOException {
    StringBuilder report = new StringBuilder("# Template render report\n\n");
    report.append("Severity: injection (raw in HTML), breakout (raw in a script), apostrophe (HTML-escaped but ' kept, ")
      .append("breaks single-quoted attributes), display (encoded for another context, shows entities), check (other ")
      .append("encoding, e.g. truncated or URL-encoded), vue (in a Vue app without v-pre: {{ }} in the value runs).\n\n");

    report.append("## Pages\n\n| Page | Markers | Findings | Errors | Size |\n|---|---|---|---|---|\n");
    for (PageResult result : results) {
      report.append("| ").append(result.page()).append(" | ").append(result.markers()).append(" | ")
        .append(result.findings().size()).append(" | ").append(result.errors().size()).append(" | ")
        .append(result.output().length()).append(" |\n");
    }

    // a value read in a shared template is found on many pages: grouped by template, path and kind
    Map<String, Map<String, Integer>> grouped = new TreeMap<>();
    for (PageResult result : results) {
      for (Finding finding : result.findings()) {
        String key = finding.context() + " | " + finding.encoding() + " | " + severity(finding);
        grouped.computeIfAbsent(finding.template() + " | `" + finding.path() + "`", k -> new TreeMap<>())
          .merge(key, 1, Integer::sum);
      }
    }
    report.append("\n## Findings\n\n| Template | Path | Context | Encoding | Severity | Count |\n|---|---|---|---|---|---|\n");
    grouped.forEach((location, kinds) -> kinds.forEach((kind, count) ->
      report.append("| ").append(location).append(" | ").append(kind).append(" | ").append(count).append(" |\n")));

    Map<String, Integer> links = new TreeMap<>();
    scriptLinks.forEach(link -> links.merge(link, 1, Integer::sum));
    report.append("\n## Links without safeUrl()\n\n| Template | Path | Count |\n|---|---|---|\n");
    links.forEach((link, count) -> report.append("| ").append(link).append(" | ").append(count).append(" |\n"));

    report.append("\n## Errors\n\n");
    for (PageResult result : results) {
      if (result.errors().isEmpty()) continue;
      report.append("### ").append(result.page()).append("\n\n");
      result.errors().stream().distinct().forEach(error -> report.append("- ").append(error).append("\n"));
      report.append("\n");
    }
    Files.writeString(OUTPUT.resolve("report.md"), report.toString(), StandardCharsets.UTF_8);
  }

  //
  // Hostile model
  //

  /**
   * The stubs of one rendering: each one gets an id, printed in its marker, that gives back where it was read.
   */
  private static class Stubs {

    private final ObjectWrapper wrapper;

    private final boolean scriptLinks;

    private final List<Origin> origins = new ArrayList<>();

    private final Properties messages;

    private final boolean synthetic;

    Stubs(ObjectWrapper wrapper, boolean scriptLinks, String language) throws IOException {
      this.wrapper = wrapper;
      this.scriptLinks = scriptLinks;
      this.messages = translations(language);
      this.synthetic = SYNTHETIC.equals(language);
    }

    TemplateHashModel root() {
      Stubs stubs = this;
      return new TemplateHashModel() {
        @Override
        public TemplateModel get(String key) throws TemplateModelException {
          // shared variables come after the data model: they must not be hidden
          if (HtmlDecodeMethod.NAME.equals(key) || TemplateSafeMethod.NAME.equals(key)) return null;
          if ("springMacroRequestContext".equals(key)) return new RequestContext(stubs);
          return child(null, key);
        }

        @Override
        public boolean isEmpty() {
          return false;
        }
      };
    }

    Origin origin(int id) {
      return id < origins.size() ? origins.get(id) : new Origin("?", "?");
    }

    TemplateModel child(Stub parent, String name) throws TemplateModelException {
      String path = parent == null ? name : parent.path + (name.startsWith("[") || name.startsWith("(") ? "" : ".") + name;
      if (OVERRIDES.containsKey(path)) return wrapper.wrap(OVERRIDES.get(path));
      String template = currentTemplate();
      if (parent != null && HELPERS.contains(template)) template = parent.template;
      origins.add(new Origin(path, template));
      return new Stub(this, origins.size() - 1, path, template, parent == null ? 1 : parent.depth + 1);
    }

    private static String currentTemplate() {
      Environment environment = Environment.getCurrentEnvironment();
      return environment == null ? "?" : environment.getCurrentTemplate().getSourceName();
    }

    String value(int id) {
      return scriptLinks ? "javascript:ZX" + id + "ZY" : "ZX" + id + PAYLOAD + "ZY";
    }

    String message(List<?> arguments) throws TemplateModelException {
      String code = string(arguments.get(0));
      String text = messages.getProperty(code);
      if (text == null && synthetic) {
        // codes not in the bundles, values (dar.status) or missing ones, are checked as well
        Matcher marker = MARKER.matcher(code);
        String name = marker.matches() ? origin(Integer.parseInt(marker.group(1))).path() : code;
        return "TQ" + name.replaceAll("[|<>\\s]", "_") + "|" + PAYLOAD + "TQ";
      }
      if (text == null) return string(arguments.get(arguments.size() - 1));
      if (arguments.size() == 3 && arguments.get(1) instanceof TemplateSequenceModel args) {
        Object[] values = new Object[args.size()];
        for (int i = 0; i < values.length; i++) values[i] = string(args.get(i));
        return MessageFormat.format(text, values);
      }
      return text;
    }

    private static String string(Object argument) throws TemplateModelException {
      return argument instanceof TemplateScalarModel scalar ? scalar.getAsString() : String.valueOf(argument);
    }
  }

  /**
   * Spring's request context: real translations, a stub for anything else.
   */
  private record RequestContext(Stubs stubs) implements TemplateHashModel {

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
      if ("getMessage".equals(key)) return (TemplateMethodModelEx) arguments -> stubs.message(arguments);
      return stubs.child(null, "springMacroRequestContext." + key);
    }

    @Override
    public boolean isEmpty() {
      return false;
    }
  }

  /**
   * A model value that fits any expression: a hash of stubs, a list of one stub, a method returning a stub, true, and
   * a marker string.
   */
  private static class Stub implements TemplateHashModelEx, TemplateSequenceModel, TemplateMethodModelEx,
    TemplateScalarModel, TemplateBooleanModel {

    private final Stubs stubs;

    private final int id;

    private final String path;

    private final String template;

    private final int depth;

    Stub(Stubs stubs, int id, String path, String template, int depth) {
      this.stubs = stubs;
      this.id = id;
      this.path = path;
      this.template = template;
      this.depth = depth;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
      return stubs.child(this, key);
    }

    @Override
    public TemplateModel get(int index) throws TemplateModelException {
      return stubs.child(this, "[" + index + "]");
    }

    @Override
    public int size() {
      return depth < MAX_DEPTH ? 1 : 0;
    }

    @Override
    public boolean isEmpty() {
      return size() == 0;
    }

    @Override
    public TemplateCollectionModel keys() {
      return collection(List.of(new SimpleScalar("en")));
    }

    @Override
    public TemplateCollectionModel values() throws TemplateModelException {
      return collection(size() == 0 ? List.of() : List.of(get("en")));
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
      return stubs.child(this, "()");
    }

    @Override
    public String getAsString() {
      return stubs.value(id);
    }

    @Override
    public boolean getAsBoolean() {
      return true;
    }

    private static TemplateCollectionModel collection(List<TemplateModel> items) {
      return () -> new TemplateModelIterator() {
        private final Iterator<TemplateModel> iterator = items.iterator();

        @Override
        public TemplateModel next() {
          return iterator.next();
        }

        @Override
        public boolean hasNext() {
          return iterator.hasNext();
        }
      };
    }
  }
}
