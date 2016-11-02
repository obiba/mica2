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

package org.obiba.mica.micaConfig.rest;

import org.junit.Test;
import org.obiba.core.translator.Translator;
import org.obiba.mica.PrefixedValueTranslator;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class PrefixedValueTranslatorTest {

  @Test
  public void when_non_prefixed_value__return_the_same_value() throws Exception {

    assertThat(prefixedSimpleTranslator().translate("toTranslate"), is("toTranslate"));
    assertThat(prefixedSimpleTranslator().translate("nonExistent"), is("nonExistent"));
  }

  @Test
  public void when_prefixed_value__return_translated_value() throws Exception {
    assertThat(prefixedSimpleTranslator().translate("t(toTranslate)"), is("translated"));
  }

  @Test
  public void when_complete_sentence_with_many_prefixes__return_translated_sentence() throws Exception {
    assertThat(prefixedSimpleTranslator().translate("Phrase with some words t(toTranslate) and t(toTranslate2)"),
      is("Phrase with some words translated and translated2"));
  }

  private Translator prefixedSimpleTranslator() {
    return new PrefixedValueTranslator(new SimpleTranslator());
  }

  private class SimpleTranslator implements Translator {

    @Override
    public String translate(String key) {
      switch (key) {
        case "toTranslate":
          return "translated";
        case "toTranslate2":
          return "translated2";
        default:
          return key;
      }
    }
  }
}
