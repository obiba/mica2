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

  // model values with a real value, as the pages compute on them
  private static final Map<String, Object> OVERRIDES = Map.of(
    "contextPath", "",
    "config.contextPath", "",
    "user.roles", List.of("mica-administrator", "mica-reviewer", "mica-editor", "mica-data-access-officer"));

  // helper templates: a value read there is reported where its parent was read (e.g. localize(study.name))
  private static final List<String> HELPERS = List.of("libs/head.ftl");

  // stops recursive macros (trees): lists are empty from this path depth
  private static final int MAX_DEPTH = 6;

  enum Context {HTML, SCRIPT, TEMPLATE}

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
    Stubs stubs = new Stubs(configuration.getObjectWrapper(), scriptLinks);
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
    }
    return new PageResult(page, output, markers, findings, errors);
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
    };
  }

  private static String severity(Finding finding) {
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
  // Report
  //

  private void writeReport(List<PageResult> results, List<String> scriptLinks) throws IOException {
    StringBuilder report = new StringBuilder("# Template render report\n\n");
    report.append("Severity: injection (raw in HTML), breakout (raw in a script), apostrophe (HTML-escaped but ' kept, ")
      .append("breaks single-quoted attributes), display (encoded for another context, shows entities), check (other ")
      .append("encoding, e.g. truncated or URL-encoded).\n\n");

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

    private final Properties messages = new Properties();

    Stubs(ObjectWrapper wrapper, boolean scriptLinks) throws IOException {
      this.wrapper = wrapper;
      this.scriptLinks = scriptLinks;
      try (InputStream input = TemplatesRenderTest.class.getResourceAsStream("/i18n/messages_en.properties")) {
        messages.load(new InputStreamReader(input, StandardCharsets.UTF_8));
      }
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
