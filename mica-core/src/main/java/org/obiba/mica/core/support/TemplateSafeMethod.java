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

import java.util.List;
import java.util.regex.Pattern;

import freemarker.core.HTMLOutputFormat;
import freemarker.core.TemplateMarkupOutputModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

/**
 * FreeMarker method for the Markdown source that a page puts in a template block, to be rendered and sanitized in the
 * browser (renderMarkdown in mica.js): <code>&lt;template&gt;${templateSafe(value)}&lt;/template&gt;</code>.
 * <p>
 * The source is kept as typed, except the closing template tag: it would end the block early and what follows would be
 * parsed as live HTML, before the sanitized rendering.
 * <p>
 * The result is HTML markup output: it is printed as is whether the templates are auto-escaped or not, no
 * <code>?no_esc</code> is needed. Values that are not strings are returned unchanged, a missing value gives an empty
 * text.
 * <p>
 * It is a shared variable and not a function of libs/head.ftl, so that it exists when a custom libs/head.ftl
 * (MICA_HOME/conf/templates) replaces the built-in one.
 */
public class TemplateSafeMethod implements TemplateMethodModelEx {

  public static final String NAME = "templateSafe";

  // the tag name ends at a space, a slash or ">": matching the prefix covers "</template >", "</TEMPLATE\n>"...
  private static final Pattern TEMPLATE_END_TAG = Pattern.compile("</template", Pattern.CASE_INSENSITIVE);

  @Override
  public Object exec(List arguments) throws TemplateModelException {
    if (arguments.size() != 1) {
      throw new TemplateModelException(NAME + " expects one argument");
    }
    Object argument = arguments.get(0);
    if (argument == null) {
      return HTMLOutputFormat.INSTANCE.fromMarkup("");
    }
    if (argument instanceof TemplateScalarModel scalar) {
      return HTMLOutputFormat.INSTANCE.fromMarkup(neutralize(scalar.getAsString()));
    }
    if (argument instanceof TemplateMarkupOutputModel<?> markup) {
      // already markup (e.g. value?no_esc in an auto-escaped template)
      return HTMLOutputFormat.INSTANCE.fromMarkup(neutralize(markupString(markup)));
    }
    return argument;
  }

  /**
   * Replace the start of any closing template tag so that it is text for the HTML parser.
   *
   * @param text
   * @return the text that cannot close a template block, null if the text is null
   */
  public static String neutralize(String text) {
    if (text == null) return null;
    return TEMPLATE_END_TAG.matcher(text).replaceAll("&lt;/template");
  }

  private static <MO extends TemplateMarkupOutputModel<MO>> String markupString(TemplateMarkupOutputModel<MO> markup)
    throws TemplateModelException {
    @SuppressWarnings("unchecked")
    MO model = (MO) markup;
    return model.getOutputFormat().getMarkupString(model);
  }
}
