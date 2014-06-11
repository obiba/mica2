package org.obiba.mica.web.model;

import java.util.Locale;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.MicaConfigService;
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
