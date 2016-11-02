/*
 *
 *  * Copyright (c) 2016 OBiBa. All rights reserved.
 *  *
 *  * This program and the accompanying materials
 *  * are made available under the terms of the GNU Public License v3.0.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.obiba.mica;

import org.obiba.core.translator.Translator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrefixedValueTranslator implements Translator {

  private static final Pattern pattern = Pattern.compile("t\\(([^\\)]+)\\)");
  private Translator translator;

  public PrefixedValueTranslator(Translator translator) {
    this.translator = translator;
  }

  @Override
  public String translate(String prefixedValueToTranslate) {

    Matcher matcher = pattern.matcher(prefixedValueToTranslate);
    while (matcher.find()) {
      String word = matcher.group(1);
      String translatedWord = translator.translate(word);
      prefixedValueToTranslate = prefixedValueToTranslate.replace("t(" + word + ")", translatedWord);
    }

    return prefixedValueToTranslate;
  }
}
