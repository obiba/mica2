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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.LocalizedString;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
public class LocalizedStringDtos {

  Iterable<Mica.LocalizedStringDto> asDto(@SuppressWarnings("TypeMayBeWeakened") LocalizedString localizedString) {
    if (localizedString == null) return Collections.emptyList();
    return localizedString.entrySet().stream().map(
        entry -> Mica.LocalizedStringDto.newBuilder().setLang(entry.getKey()).setValue(entry.getValue())
            .build()
    ).collect(Collectors.toList());
  }

  LocalizedString fromDto(@Nullable Collection<Mica.LocalizedStringDto> dtos) {
    if(dtos == null || dtos.isEmpty()) return null;
    LocalizedString localizedString = new LocalizedString();
    for(Mica.LocalizedStringDto dto : dtos) {
      if(!Strings.isNullOrEmpty(dto.getValue())) {
        localizedString.put(dto.getLang(), dto.getValue());
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

  public Iterable<Mica.LocalizedStringDto> asDto(Map<String, String> localizedMap) {
    return asDto(localizedMap, null);
  }

  /**
   *
   * @param localizedMap
   * @param locale Preferred locale if exists, otherwise all locales are returned
   * @return
   */
  public Iterable<Mica.LocalizedStringDto> asDto(Map<String, String> localizedMap, @Nullable String locale) {
    if (localizedMap == null || localizedMap.isEmpty()) return Collections.emptyList();
    if (locale != null && localizedMap.containsKey(locale)) {
      return Collections.singleton(Mica.LocalizedStringDto.newBuilder().setLang(locale).setValue(localizedMap.get(locale))
        .build());
    }
    return localizedMap.entrySet().stream().map(
        entry -> Mica.LocalizedStringDto.newBuilder().setLang(entry.getKey()).setValue(entry.getValue())
            .build()
    ).collect(Collectors.toList());
  }
}
