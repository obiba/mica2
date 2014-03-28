package org.obiba.mica.web.model;

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Address;
import org.obiba.mica.domain.Contact;
import org.obiba.mica.domain.LocalizedString;
import org.obiba.mica.domain.Timestamped;
import org.obiba.mica.domain.study.Study;
import org.springframework.stereotype.Component;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.obiba.mica.domain.Contact.Institution;
import static org.obiba.mica.web.model.Mica.AddressDto;
import static org.obiba.mica.web.model.Mica.AddressDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.ContactDto;
import static org.obiba.mica.web.model.Mica.ContactDto.InstitutionDto;
import static org.obiba.mica.web.model.Mica.ContactDto.InstitutionDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.ContactDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.LocalizedStringDto;
import static org.obiba.mica.web.model.Mica.StudyDto;
import static org.obiba.mica.web.model.Mica.StudyDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.TimestampsDto;

@Component
@SuppressWarnings("OverlyCoupledClass")
public class Dtos {

  @NotNull
  public StudyDto asDto(@NotNull Study study) {
    StudyDto.Builder builder = StudyDto.newBuilder();
    builder.setId(study.getId()) //
        .setTimestamps(asDto((Timestamped) study)) //
        .addAllName(asDtos(study.getName())) //
        .addAllObjectives(asDtos(study.getObjectives()));

    if(study.getAcronym() != null) builder.addAllAcronym(asDtos(study.getAcronym()));
    if(study.getInvestigators() != null) {
      builder.addAllInvestigators(study.getInvestigators().stream().map(this::asDto).collect(Collectors.toList()));
    }
    if(study.getContacts() != null) {
      builder.addAllContacts(study.getContacts().stream().map(this::asDto).collect(Collectors.toList()));
    }
    if(!isNullOrEmpty(study.getWebsite())) builder.setWebsite(study.getWebsite());

    //TODO continue

    return builder.build();
  }

  @NotNull
  public Study fromDto(@NotNull StudyDtoOrBuilder dto) {
    Study study = new Study();
    study.setId(dto.getId());
    study.setName(fromDto(dto.getNameList()));
    study.setAcronym(fromDto(dto.getAcronymList()));
    study.setInvestigators(dto.getInvestigatorsList().stream().map(this::fromDto).collect(Collectors.toList()));
    study.setContacts(dto.getContactsList().stream().map(this::fromDto).collect(Collectors.toList()));
    study.setObjectives(fromDto(dto.getObjectivesList()));
    if(dto.hasWebsite()) study.setWebsite(dto.getWebsite());

    //TODO continue

    return study;
  }

  private TimestampsDto asDto(Timestamped timestamped) {
    TimestampsDto.Builder builder = TimestampsDto.newBuilder().setCreated(timestamped.getCreated().toString());
    if(timestamped.getUpdated() != null) builder.setLastUpdate(timestamped.getUpdated().toString());
    return builder.build();
  }

  private Iterable<LocalizedStringDto> asDtos(@SuppressWarnings("TypeMayBeWeakened") LocalizedString localizedString) {
    return localizedString.entrySet().stream().map(
        entry -> LocalizedStringDto.newBuilder().setLang(entry.getKey().getLanguage()).setValue(entry.getValue())
            .build()
    ).collect(Collectors.toList());
  }

  private LocalizedString fromDto(@Nullable Collection<LocalizedStringDto> dtos) {
    if(dtos == null || dtos.isEmpty()) return null;
    LocalizedString localizedString = new LocalizedString();
    dtos.forEach(dto -> localizedString.put(new Locale(dto.getLang()), dto.getValue()));
    return localizedString;
  }

  private ContactDto asDto(Contact contact) {
    ContactDto.Builder builder = ContactDto.newBuilder().setLastName(contact.getLastName());
    if(!isNullOrEmpty(contact.getTitle())) builder.setTitle(contact.getTitle());
    if(!isNullOrEmpty(contact.getFirstName())) builder.setFirstName(contact.getFirstName());
    if(!isNullOrEmpty(contact.getEmail())) builder.setEmail(contact.getEmail());
    if(!isNullOrEmpty(contact.getPhone())) builder.setPhone(contact.getPhone());
    if(contact.getInstitution() != null) builder.setInstitution(asDto(contact.getInstitution()));
    return builder.build();
  }

  private Contact fromDto(ContactDtoOrBuilder dto) {
    Contact contact = new Contact();
    if(dto.hasTitle()) contact.setTitle(dto.getTitle());
    if(dto.hasFirstName()) contact.setFirstName(dto.getFirstName());
    contact.setLastName(dto.getLastName());
    if(dto.hasEmail()) contact.setEmail(dto.getEmail());
    if(dto.hasPhone()) contact.setPhone(dto.getPhone());
    if(dto.hasInstitution()) contact.setInstitution(fromDto(dto.getInstitution()));
    return contact;
  }

  private InstitutionDto asDto(Institution institution) {
    InstitutionDto.Builder builder = InstitutionDto.newBuilder();
    if(institution.getName() != null) builder.addAllName(asDtos(institution.getName()));
    if(institution.getDepartment() != null) builder.addAllDepartment(asDtos(institution.getDepartment()));
    if(institution.getAddress() != null) builder.setAddress(asDto(institution.getAddress()));
    return builder.build();
  }

  private Institution fromDto(InstitutionDtoOrBuilder dto) {
    Institution institution = new Institution();
    institution.setName(fromDto(dto.getNameList()));
    institution.setDepartment(fromDto(dto.getDepartmentList()));
    if(dto.hasAddress()) institution.setAddress(fromDto(dto.getAddress()));
    return institution;
  }

  private AddressDto asDto(Address address) {
    AddressDto.Builder builder = AddressDto.newBuilder();
    if(address.getStreet() != null) builder.addAllStreet(asDtos(address.getStreet()));
    if(address.getCity() != null) builder.addAllCity(asDtos(address.getCity()));
    if(!isNullOrEmpty(address.getZip())) builder.setZip(address.getZip());
    if(!isNullOrEmpty(address.getState())) builder.setState(address.getState());
    if(!isNullOrEmpty(address.getCountryIso())) builder.setCountryIso(address.getCountryIso());
    return builder.build();
  }

  private Address fromDto(AddressDtoOrBuilder dto) {
    Address address = new Address();
    address.setStreet(fromDto(dto.getStreetList()));
    address.setCity(fromDto(dto.getCityList()));
    if(dto.hasZip()) address.setZip(dto.getZip());
    if(dto.hasState()) address.setState(dto.getState());
    if(dto.hasCountryIso()) address.setCountryIso(dto.getCountryIso());
    return address;
  }

}
