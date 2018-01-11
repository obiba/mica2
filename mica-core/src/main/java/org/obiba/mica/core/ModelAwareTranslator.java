/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core;

import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.TranslationUtils;
import org.obiba.core.translator.Translator;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.core.domain.ModelAware;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ModelAwareTranslator {

  @Autowired
  private MicaConfigService micaConfigService;

  public <T extends ModelAware> void translateModel(String locale, T modelAwareObject) {

    if (locale == null)
      return;

    Translator translator = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(locale, false));
    new ForLocale(translator).translateModel(modelAwareObject);
  }

  public ForLocale getModelAwareTranslatorForLocale(String locale) {
    return new ForLocale(JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(locale, false)));
  }

  public class ForLocale {

    private Translator translator;

    ForLocale(Translator translator) {
      this.translator = translator;
    }

    public <T extends ModelAware> void translateModel(T modelAwareObject) {
      String jsonModel = JSONUtils.toJSON(modelAwareObject.getModel());
      String translated = new TranslationUtils().translate(jsonModel, translator);
      modelAwareObject.setModel(JSONUtils.toMap(translated));
    }
  }
}
