package org.obiba.mica.web.model;

import org.obiba.mica.domain.Address;
import org.obiba.mica.domain.Contact;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.obiba.mica.web.model.LocalizedStringDtos.asDtos;

class ContactDtos {

  private ContactDtos() {}

  static Mica.ContactDto asDto(Contact contact) {
    Mica.ContactDto.Builder builder = Mica.ContactDto.newBuilder().setLastName(contact.getLastName());
    if(!isNullOrEmpty(contact.getTitle())) builder.setTitle(contact.getTitle());
    if(!isNullOrEmpty(contact.getFirstName())) builder.setFirstName(contact.getFirstName());
    if(!isNullOrEmpty(contact.getEmail())) builder.setEmail(contact.getEmail());
    if(!isNullOrEmpty(contact.getPhone())) builder.setPhone(contact.getPhone());
    if(contact.getInstitution() != null) builder.setInstitution(asDto(contact.getInstitution()));
    return builder.build();
  }

  static Contact fromDto(Mica.ContactDtoOrBuilder dto) {
    Contact contact = new Contact();
    if(dto.hasTitle()) contact.setTitle(dto.getTitle());
    if(dto.hasFirstName()) contact.setFirstName(dto.getFirstName());
    contact.setLastName(dto.getLastName());
    if(dto.hasEmail()) contact.setEmail(dto.getEmail());
    if(dto.hasPhone()) contact.setPhone(dto.getPhone());
    if(dto.hasInstitution()) contact.setInstitution(fromDto(dto.getInstitution()));
    return contact;
  }

  private static Mica.ContactDto.InstitutionDto asDto(Contact.Institution institution) {
    Mica.ContactDto.InstitutionDto.Builder builder = Mica.ContactDto.InstitutionDto.newBuilder();
    if(institution.getName() != null) builder.addAllName(asDtos(institution.getName()));
    if(institution.getDepartment() != null) builder.addAllDepartment(asDtos(institution.getDepartment()));
    if(institution.getAddress() != null) builder.setAddress(asDto(institution.getAddress()));
    return builder.build();
  }

  private static Contact.Institution fromDto(Mica.ContactDto.InstitutionDtoOrBuilder dto) {
    Contact.Institution institution = new Contact.Institution();
    institution.setName(LocalizedStringDtos.fromDto(dto.getNameList()));
    institution.setDepartment(LocalizedStringDtos.fromDto(dto.getDepartmentList()));
    if(dto.hasAddress()) institution.setAddress(fromDto(dto.getAddress()));
    return institution;
  }

  private static Mica.AddressDto asDto(Address address) {
    Mica.AddressDto.Builder builder = Mica.AddressDto.newBuilder();
    if(address.getStreet() != null) builder.addAllStreet(asDtos(address.getStreet()));
    if(address.getCity() != null) builder.addAllCity(asDtos(address.getCity()));
    if(!isNullOrEmpty(address.getZip())) builder.setZip(address.getZip());
    if(!isNullOrEmpty(address.getState())) builder.setState(address.getState());
    if(!isNullOrEmpty(address.getCountryIso())) builder.setCountryIso(address.getCountryIso());
    return builder.build();
  }

  private static Address fromDto(Mica.AddressDtoOrBuilder dto) {
    Address address = new Address();
    address.setStreet(LocalizedStringDtos.fromDto(dto.getStreetList()));
    address.setCity(LocalizedStringDtos.fromDto(dto.getCityList()));
    if(dto.hasZip()) address.setZip(dto.getZip());
    if(dto.hasState()) address.setState(dto.getState());
    if(dto.hasCountryIso()) address.setCountryIso(dto.getCountryIso());
    return address;
  }

}
