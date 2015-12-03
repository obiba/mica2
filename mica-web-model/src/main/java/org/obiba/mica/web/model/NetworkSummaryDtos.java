package org.obiba.mica.web.model;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.springframework.stereotype.Component;


@Component
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
class NetworkSummaryDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private NetworkService networkService;

  @NotNull
  public Mica.NetworkSummaryDto.Builder asDtoBuilder(@NotNull String id, boolean asDraft) {
    NetworkState networkState = networkService.getEntityState(id);
    Network network = networkService.findById(id);
    Mica.NetworkSummaryDto.Builder builder = Mica.NetworkSummaryDto.newBuilder();

    builder.setId(id).addAllAcronym(localizedStringDtos.asDto(network.getAcronym())) //
      .addAllName(localizedStringDtos.asDto(network.getName())) //
      .setPublished(networkState.isPublished());

    if(asDraft) {
      builder.setTimestamps(TimestampsDtos.asDto(network));
    }

    return builder;
  }
}
