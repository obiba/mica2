package org.obiba.mica.web.model;

import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Contact;
import org.obiba.mica.domain.Network;
import org.obiba.mica.domain.Timestamped;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.obiba.mica.web.model.Mica.ContactDto;

@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
class NetworkDtos {

  private NetworkDtos() {}

  @NotNull
  static Mica.NetworkDto asDto(@NotNull Network network) {
    Mica.NetworkDto.Builder builder = Mica.NetworkDto.newBuilder();
    builder.setId(network.getId()) //
        .setTimestamps(TimestampsDtos.asDto((Timestamped) network)) //
        .addAllName(LocalizedStringDtos.asDtos(network.getName())) //
        .addAllDescription(LocalizedStringDtos.asDtos(network.getDescription()));

    if(network.getAcronym() != null) builder.addAllAcronym(LocalizedStringDtos.asDtos(network.getAcronym()));
    if(network.getInvestigators() != null) {
      builder.addAllInvestigators(
          network.getInvestigators().stream().map(ContactDtos::asDto).collect(Collectors.<ContactDto>toList()));
    }
    if(network.getContacts() != null) {
      builder.addAllContacts(
          network.getContacts().stream().map(ContactDtos::asDto).collect(Collectors.<ContactDto>toList()));
    }
    if(!isNullOrEmpty(network.getWebsite())) builder.setWebsite(network.getWebsite());

    //TODO continue

    return builder.build();
  }

  @NotNull
  static Network fromDto(@NotNull Mica.NetworkDtoOrBuilder dto) {
    Network network = new Network();
    network.setId(dto.getId());
    network.setName(LocalizedStringDtos.fromDto(dto.getNameList()));
    network.setAcronym(LocalizedStringDtos.fromDto(dto.getAcronymList()));
    network.setInvestigators(
        dto.getInvestigatorsList().stream().map(ContactDtos::fromDto).collect(Collectors.<Contact>toList()));
    network.setContacts(dto.getContactsList().stream().map(ContactDtos::fromDto).collect(Collectors.<Contact>toList()));
    network.setDescription(LocalizedStringDtos.fromDto(dto.getDescriptionList()));
    if(dto.hasWebsite()) network.setWebsite(dto.getWebsite());

    //TODO continue

    return network;
  }

}
