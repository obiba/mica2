package org.obiba.mica.web.model;

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.obiba.mica.domain.LocalizedString;

class LocalizedStringDtos {

  private LocalizedStringDtos() {}

  static Iterable<Mica.LocalizedStringDto> asDtos(
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

}
