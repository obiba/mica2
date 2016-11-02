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

import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.TranslationUtils;
import org.obiba.core.translator.Translator;
import org.obiba.mica.PrefixedValueTranslator;
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EntityConfigTranslator {

  @Autowired
  private MicaConfigService micaConfigService;

  public <T extends EntityConfig> void translateSchema(String locale, T entityConfig) {

    if (StringUtils.isEmpty(locale))
      return;

    Translator translator = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(locale, false));
    translator = new PrefixedValueTranslator(translator);

    String translated = new TranslationUtils().translate(entityConfig.getSchema(), translator);
    entityConfig.setSchema(translated);
  }
}
