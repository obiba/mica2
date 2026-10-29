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

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import org.jsoup.parser.Parser;

/**
 * FreeMarker method that decodes the HTML entities of a value that may be stored HTML-encoded, so that it can be
 * HTML-escaped exactly once in the template: <code>${htmlDecode(value)?html}</code>.
 * <p>
 * List names are stored raw before Mica 6.0, encoded since 6.0, and set operation names contain entities on purpose
 * (<code>S1 &amp;Union; S2</code>): decoding them all gives the text to display. Values that are not strings are
 * returned unchanged.
 */
public class HtmlDecodeMethod implements TemplateMethodModelEx {

  public static final String NAME = "htmlDecode";

  @Override
  public Object exec(List arguments) throws TemplateModelException {
    if (arguments.size() != 1) {
      throw new TemplateModelException(NAME + " expects one argument");
    }
    Object argument = arguments.get(0);
    if (argument instanceof TemplateScalarModel scalar) {
      return decode(scalar.getAsString());
    }
    return argument;
  }

  /**
   * Decode the HTML entities (HTML5 named and numeric) of a text, plain text is returned unchanged. Decoding as in a
   * text content, the same as the browser does (decodeHtml in mica.js): a legacy entity without semicolon is decoded
   * (e.g. "R&copy2" gives "R©2").
   *
   * @param text
   * @return the decoded text, null if the text is null
   */
  public static String decode(String text) {
    if (text == null) return null;
    return Parser.unescapeEntities(text, false);
  }
}
