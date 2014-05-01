package org.obiba.mica.web.model;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Contact;
import org.obiba.mica.domain.Network;
import org.obiba.mica.domain.Timestamped;
import org.springframework.stereotype.Component;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.obiba.mica.web.model.Mica.ContactDto;

@Component
class NetworkDtos {

  @Inject
  private ContactDtos contactDtos;

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @NotNull
  Mica.NetworkDto asDto(@NotNull Network network) {
    Mica.NetworkDto.Builder builder = Mica.NetworkDto.newBuilder();
    builder.setId(network.getId()) //
        .setTimestamps(TimestampsDtos.asDto((Timestamped) network)) //
        .addAllName(localizedStringDtos.asDto(network.getName())) //
        .addAllDescription(localizedStringDtos.asDto(network.getDescription()));

    if(network.getAcronym() != null) builder.addAllAcronym(localizedStringDtos.asDto(network.getAcronym()));
    if(network.getInvestigators() != null) {
      builder.addAllInvestigators(
          network.getInvestigators().stream().map(contactDtos::asDto).collect(Collectors.<ContactDto>toList()));
    }
    if(network.getContacts() != null) {
      builder.addAllContacts(
          network.getContacts().stream().map(contactDtos::asDto).collect(Collectors.<ContactDto>toList()));
    }
    if(!isNullOrEmpty(network.getWebsite())) builder.setWebsite(network.getWebsite());

    //TODO continue

    return builder.build();
  }

  @NotNull
  Network fromDto(@NotNull Mica.NetworkDtoOrBuilder dto) {
    Network network = new Network();
    network.setId(dto.getId());
    network.setName(localizedStringDtos.fromDto(dto.getNameList()));
    network.setAcronym(localizedStringDtos.fromDto(dto.getAcronymList()));
    network.setInvestigators(
        dto.getInvestigatorsList().stream().map(contactDtos::fromDto).collect(Collectors.<Contact>toList()));
    network.setContacts(dto.getContactsList().stream().map(contactDtos::fromDto).collect(Collectors.<Contact>toList()));
    network.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    if(dto.hasWebsite()) network.setWebsite(dto.getWebsite());

    //TODO continue

    return network;
  }

}
