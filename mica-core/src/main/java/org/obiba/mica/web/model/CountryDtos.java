/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.util.Locale;

import jakarta.inject.Inject;

import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.stereotype.Component;

@Component
public class CountryDtos {

  @Inject
  private MicaConfigService micaConfigService;

  Mica.CountryDto asDto(String countryIso) {
    Mica.CountryDto.Builder builder = Mica.CountryDto.newBuilder().setIso(countryIso);
    micaConfigService.getConfig().getLocales().forEach(locale -> builder.addName(
        Mica.LocalizedStringDto.newBuilder().setLang(locale.getLanguage())
            .setValue(new Locale(countryIso).getDisplayCountry(locale))));
    return builder.build();
  }

}
