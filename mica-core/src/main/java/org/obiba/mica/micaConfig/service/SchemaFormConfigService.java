/*
 * Copyright (c) 2024 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.Translator;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.obiba.mica.micaConfig.service.helper.SchemaFormConfig;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class SchemaFormConfigService {

  @Inject
  private MicaConfigService micaConfigService;

  public SchemaFormConfig getConfig(AbstractDataAccessEntityForm form, DataAccessEntity request, String lang) {
    return new SchemaFormConfig(micaConfigService, form.getSchema(), form.getDefinition(), request.getContent(), lang, true);
  }

  public Translator getTranslator(String lang) {
    return JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(lang, false));
  }

}
