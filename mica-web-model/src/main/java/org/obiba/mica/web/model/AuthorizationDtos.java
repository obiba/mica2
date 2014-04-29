package org.obiba.mica.web.model;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Authorization;

import com.google.common.base.Strings;

import static org.obiba.mica.web.model.Mica.AuthorizationDto;

class AuthorizationDtos {

  private AuthorizationDtos() {}

  @NotNull
  static AuthorizationDto asDto(@NotNull Authorization authorization) {
    AuthorizationDto.Builder builder = AuthorizationDto.newBuilder();
    builder.setAuthorized(authorization.isAuthorized());
    if (!Strings.isNullOrEmpty(authorization.getAuthorizer())) builder.setAuthorizer(authorization.getAuthorizer());
    if (authorization.getDate() != null) builder.setDate(authorization.getDate().toString());

    return builder.build();
  }

  @NotNull
  static Authorization fromDto(@NotNull AuthorizationDto dto) {
    Authorization authorization = new Authorization();
    if (dto.hasAuthorizer()) authorization.setAuthorizer(dto.getAuthorizer());
    if (dto.hasAuthorized()) authorization.setAuthorized(dto.getAuthorized());
    if (dto.hasDate()) authorization.setDate(LocalDateTime.parse(dto.getDate()));

    return authorization;
  }

}
