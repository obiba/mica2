package org.obiba.mica.web.model;

import java.util.Collections;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Address;
import org.obiba.mica.domain.Contact;
import org.obiba.mica.domain.LocalizedString;
import org.obiba.mica.domain.study.Study;
import org.springframework.stereotype.Component;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.obiba.mica.domain.Contact.Institution;
import static org.obiba.mica.web.model.Mica.AddressDto;
import static org.obiba.mica.web.model.Mica.ContactDto;
import static org.obiba.mica.web.model.Mica.ContactDto.InstitutionDto;
import static org.obiba.mica.web.model.Mica.LocalizedStringDto;
import static org.obiba.mica.web.model.Mica.StudyDto;

@Component
public class Dtos {

  @NotNull
  public StudyDto asDto(@NotNull Study study) {
    StudyDto.Builder builder = StudyDto.newBuilder();
    builder.setId(study.getId()) //
        .addAllName(asDtos(study.getName())) //
        .addAllAcronym(asDtos(study.getAcronym())) //
        .addAllInvestigators(study.getInvestigators().stream().map(this::asDto).collect(Collectors.toList())) //
        .addAllContacts(study.getContacts().stream().map(this::asDto).collect(Collectors.toList())) //
        .addAllObjectives(asDtos(study.getObjectives()));
    if(!isNullOrEmpty(study.getWebsite())) builder.setWebsite(study.getWebsite());

    //TODO continue

    return builder.build();
  }

  @NotNull
  public Study fromDto(@NotNull StudyDto studyDto) {

    return null;
  }

  private Iterable<LocalizedStringDto> asDtos(@SuppressWarnings("TypeMayBeWeakened") LocalizedString localizedString) {
    return localizedString == null
        ? Collections.emptyList()
        : localizedString.entrySet().stream().map(
            entry -> LocalizedStringDto.newBuilder().setLang(entry.getKey().getLanguage()).setValue(entry.getValue())
                .build()
        ).collect(Collectors.toList());
  }

  @SuppressWarnings("TypeMayBeWeakened")
  private ContactDto asDto(Contact contact) {
    ContactDto.Builder builder = ContactDto.newBuilder().setLastName(contact.getLastName());
    if(!isNullOrEmpty(contact.getTitle())) builder.setTitle(contact.getTitle());
    if(!isNullOrEmpty(contact.getFirstName())) builder.setFirstName(contact.getFirstName());
    if(!isNullOrEmpty(contact.getEmail())) builder.setEmail(contact.getEmail());
    if(!isNullOrEmpty(contact.getPhone())) builder.setPhone(contact.getPhone());
    if(contact.getInstitution() != null) builder.setInstitution(asDto(contact.getInstitution()));
    return builder.build();
  }

  private InstitutionDto asDto(Institution institution) {
    InstitutionDto.Builder builder = InstitutionDto.newBuilder() //
        .addAllName(asDtos(institution.getName())) //
        .addAllDepartment(asDtos(institution.getDepartment()));
    if(institution.getAddress() != null) builder.setAddress(asDto(institution.getAddress()));
    return builder.build();
  }

  private AddressDto asDto(Address address) {
    AddressDto.Builder builder = AddressDto.newBuilder() //
        .addAllStreet(asDtos(address.getStreet())) //
        .addAllCity(asDtos(address.getCity()));
    if(!isNullOrEmpty(address.getZip())) builder.setZip(address.getZip());
    if(!isNullOrEmpty(address.getState())) builder.setState(address.getState());
    if(!isNullOrEmpty(address.getCountryIso())) builder.setCountryIso(address.getCountryIso());
    return builder.build();
  }

}
