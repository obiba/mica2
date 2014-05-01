package org.obiba.mica.web.model;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.LocalizedString;
import org.obiba.mica.service.MicaConfigService;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
class LocalizedStringDtos {

  @Inject
  private MicaConfigService micaConfigService;

  Iterable<Mica.LocalizedStringDto> asDto(@SuppressWarnings("TypeMayBeWeakened") LocalizedString localizedString) {
    return micaConfigService.getConfig().getLocalesAsString().stream().map(locale -> asDto(locale, localizedString)).
        collect(Collectors.toList());
  }

  private Mica.LocalizedStringDto asDto(String locale,
      @SuppressWarnings("TypeMayBeWeakened") LocalizedString localizedString) {
    Mica.LocalizedStringDto.Builder builder = Mica.LocalizedStringDto.newBuilder().setLang(locale);
    String value = localizedString.get(new Locale(locale));
    if(value != null) builder.setValue(value);
    return builder.build();
  }

  LocalizedString fromDto(@Nullable Collection<Mica.LocalizedStringDto> dtos) {
    if(dtos == null || dtos.isEmpty()) return null;
    LocalizedString localizedString = new LocalizedString();
    for(Mica.LocalizedStringDto dto : dtos) {
      if(!Strings.isNullOrEmpty(dto.getValue())) {
        localizedString.put(new Locale(dto.getLang()), dto.getValue());
      }
    }
    return localizedString;
  }

  @NotNull
  List<Mica.LocalizedStringDtos> asDtoList(@NotNull Collection<LocalizedString> localizedStrings) {
    return localizedStrings.stream().map(
        localizedString -> Mica.LocalizedStringDtos.newBuilder().addAllLocalizedStrings(asDto(localizedString)).build())
        .collect(Collectors.toList());
  }

  @NotNull
  List<LocalizedString> fromDtoList(@NotNull Collection<Mica.LocalizedStringDtos> dtos) {
    return dtos.stream().map(dto -> fromDto(dto.getLocalizedStringsList())).collect(Collectors.toList());
  }
}
