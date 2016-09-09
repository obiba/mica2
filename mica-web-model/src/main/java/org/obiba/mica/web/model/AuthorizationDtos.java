/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.Authorization;

import com.google.common.base.Strings;

import static org.obiba.mica.web.model.Mica.AuthorizationDto;

class AuthorizationDtos {

  private AuthorizationDtos() {}

  @NotNull
  static AuthorizationDto asDto(@NotNull Authorization authorization) {
    AuthorizationDto.Builder builder = AuthorizationDto.newBuilder();
    builder.setAuthorized(authorization.isAuthorized());
    if(!Strings.isNullOrEmpty(authorization.getAuthorizer())) builder.setAuthorizer(authorization.getAuthorizer());
    if(authorization.getDate() != null)
      builder.setDate(authorization.asLocalDate().toString());
    return builder.build();
  }

  @NotNull
  static Authorization fromDto(@NotNull Mica.AuthorizationDtoOrBuilder dto) {
    Authorization authorization = new Authorization();
    if(dto.hasAuthorizer()) authorization.setAuthorizer(dto.getAuthorizer());
    if(dto.hasAuthorized()) authorization.setAuthorized(dto.getAuthorized());
    if(dto.hasDate()) {
      LocalDate date = getISO8601Date(dto.getDate());
      if(date == null) date = getEpochDate(dto.getDate());
      if(date != null) authorization.setDate(date);
    }

    return authorization;
  }

  private static LocalDate getISO8601Date(String date) {
    try {
      String day = date;
      int tz = date.indexOf('T');
      if(tz > -1) {
        day = date.substring(0, tz);
      }
      return LocalDate.parse(day);
    } catch(DateTimeParseException e) {
      return null;
    }
  }

  private static LocalDate getEpochDate(String date) {
    try {
      return LocalDate.ofEpochDay(Long.parseLong(date));
    } catch(NumberFormatException e) {
      return null;
    }
  }

}
