package org.obiba.mica.web.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.LocalizedString;

class LocalizedStringDtos {

  private LocalizedStringDtos() {}

  static Iterable<Mica.LocalizedStringDto> asDto(
      @SuppressWarnings("TypeMayBeWeakened") LocalizedString localizedString) {
    return localizedString.entrySet().stream().map(
        entry -> Mica.LocalizedStringDto.newBuilder().setLang(entry.getKey().getLanguage()).setValue(entry.getValue())
            .build()
    ).collect(Collectors.toList());
  }

  static LocalizedString fromDto(@Nullable Collection<Mica.LocalizedStringDto> dtos) {
    if(dtos == null || dtos.isEmpty()) return null;
    LocalizedString localizedString = new LocalizedString();
    dtos.forEach(dto -> localizedString.put(new Locale(dto.getLang()), dto.getValue()));
    return localizedString;
  }

  @NotNull
  static List<Mica.LocalizedStringDtos> asDtoList(@NotNull Collection<LocalizedString> localizedStrings) {
    List<Mica.LocalizedStringDtos> dtos = new ArrayList<>();
    localizedStrings.stream().forEach(localizedString -> dtos.add(
        Mica.LocalizedStringDtos.newBuilder().addAllLocalizedStrings(asDto(localizedString))
            .build()));

    return dtos;
  }

  @NotNull
  static List<LocalizedString> fromDtoList(@NotNull Collection<Mica.LocalizedStringDtos> dtos) {
    List<LocalizedString> localizedStrings = new ArrayList<>();
    dtos.stream().forEach( t -> localizedStrings.add(fromDto(t.getLocalizedStringsList())));
    return localizedStrings;
  }
}
