package org.obiba.mica.web.model;

import javax.inject.Inject;

import org.obiba.mica.core.domain.Address;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.StudyService;
import org.springframework.stereotype.Component;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;

@Component
@SuppressWarnings("OverlyCoupledClass")
class ContactDtos {

  @Inject
  private CountryDtos countryDtos;

  @Inject
  private StudyService studyService;

  @Inject
  private NetworkService networkService;

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  Mica.PersonDto asDto(Person contact) {
    Mica.PersonDto.Builder builder = Mica.PersonDto.newBuilder().setLastName(contact.getLastName());
    if(!isNullOrEmpty(contact.getId())) builder.setId(contact.getId());
    if(!isNullOrEmpty(contact.getTitle())) builder.setTitle(contact.getTitle());
    if(!isNullOrEmpty(contact.getFirstName())) builder.setFirstName(contact.getFirstName());
    if(!isNullOrEmpty(contact.getAcademicLevel())) builder.setAcademicLevel(contact.getAcademicLevel());
    if(!isNullOrEmpty(contact.getEmail())) builder.setEmail(contact.getEmail());
    if(!isNullOrEmpty(contact.getPhone())) builder.setPhone(contact.getPhone());
    if(contact.getInstitution() != null) builder.setInstitution(asDto(contact.getInstitution()));
    builder
      .addAllStudyMemberships(contact.getStudyMemberships().stream().map(this::asStudyMembershipDto).collect(toList()));
    builder.addAllNetworkMemberships(
      contact.getNetworkMemberships().stream().map(this::asNetworkMembershipDto).collect(toList()));

    return builder.build();
  }

  Person fromDto(Mica.PersonDtoOrBuilder dto) {
    Person contact = new Person();
    if(dto.hasId()) contact.setId(dto.getId());
    if(dto.hasTitle()) contact.setTitle(dto.getTitle());
    if(dto.hasFirstName()) contact.setFirstName(dto.getFirstName());
    contact.setLastName(dto.getLastName());
    if(dto.hasAcademicLevel()) contact.setAcademicLevel(dto.getAcademicLevel());
    if(dto.hasEmail()) contact.setEmail(dto.getEmail());
    if(dto.hasPhone()) contact.setPhone(dto.getPhone());
    if(dto.hasInstitution()) contact.setInstitution(fromDto(dto.getInstitution()));
    return contact;
  }

  private Mica.PersonDto.InstitutionDto asDto(Person.Institution institution) {
    Mica.PersonDto.InstitutionDto.Builder builder = Mica.PersonDto.InstitutionDto.newBuilder();
    if(institution.getName() != null) builder.addAllName(localizedStringDtos.asDto(institution.getName()));
    if(institution.getDepartment() != null) {
      builder.addAllDepartment(localizedStringDtos.asDto(institution.getDepartment()));
    }
    if(institution.getAddress() != null) builder.setAddress(asDto(institution.getAddress()));
    return builder.build();
  }

  private Mica.PersonDto.MembershipDto asStudyMembershipDto(Person.Membership membership) {
    Mica.PersonDto.MembershipDto.Builder builder = Mica.PersonDto.MembershipDto.newBuilder();
    builder.setRole(membership.getRole());
    builder.setParentId(membership.getParentId());

    if(membership.getParentId() != null) {
      Study study = studyService.findStudy(membership.getParentId());
      if (study != null) builder.addAllParentName(localizedStringDtos.asDto(study.getName()));
    }

    return builder.build();
  }

  private Mica.PersonDto.MembershipDto asNetworkMembershipDto(Person.Membership membership) {
    Mica.PersonDto.MembershipDto.Builder builder = Mica.PersonDto.MembershipDto.newBuilder();
    builder.setRole(membership.getRole());
    builder.setParentId(membership.getParentId());

    if(membership.getParentId() != null) {
      Network network = networkService.findById(membership.getParentId());
      if (network != null) builder.addAllParentName(localizedStringDtos.asDto(network.getName()));
    }

    return builder.build();
  }

  private Person.Institution fromDto(Mica.PersonDto.InstitutionDtoOrBuilder dto) {
    Person.Institution institution = new Person.Institution();
    institution.setName(localizedStringDtos.fromDto(dto.getNameList()));
    institution.setDepartment(localizedStringDtos.fromDto(dto.getDepartmentList()));
    if(dto.hasAddress()) institution.setAddress(fromDto(dto.getAddress()));
    return institution;
  }

  private Mica.AddressDto asDto(Address address) {
    Mica.AddressDto.Builder builder = Mica.AddressDto.newBuilder();
    if(address.getStreet() != null) builder.addAllStreet(localizedStringDtos.asDto(address.getStreet()));
    if(address.getCity() != null) builder.addAllCity(localizedStringDtos.asDto(address.getCity()));
    if(!isNullOrEmpty(address.getZip())) builder.setZip(address.getZip());
    if(!isNullOrEmpty(address.getState())) builder.setState(address.getState());
    if(!isNullOrEmpty(address.getCountryIso())) builder.setCountry(countryDtos.asDto(address.getCountryIso()));
    return builder.build();
  }

  private Address fromDto(Mica.AddressDtoOrBuilder dto) {
    Address address = new Address();
    address.setStreet(localizedStringDtos.fromDto(dto.getStreetList()));
    address.setCity(localizedStringDtos.fromDto(dto.getCityList()));
    if(dto.hasZip()) address.setZip(dto.getZip());
    if(dto.hasState()) address.setState(dto.getState());
    if(dto.hasCountry()) address.setCountryIso(dto.getCountry().getIso());
    return address;
  }
}
