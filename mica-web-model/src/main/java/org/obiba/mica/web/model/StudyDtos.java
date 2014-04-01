package org.obiba.mica.web.model;

import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Contact;
import org.obiba.mica.domain.Study;
import org.obiba.mica.domain.Timestamped;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.obiba.mica.web.model.Mica.ContactDto;

@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
class StudyDtos {

  private StudyDtos() {}

  @NotNull
  static Mica.StudyDto asDto(@NotNull Study study) {
    Mica.StudyDto.Builder builder = Mica.StudyDto.newBuilder();
    builder.setId(study.getId()) //
        .setTimestamps(TimestampsDtos.asDto((Timestamped) study)) //
        .addAllName(LocalizedStringDtos.asDtos(study.getName())) //
        .addAllObjectives(LocalizedStringDtos.asDtos(study.getObjectives()));

    if(study.getAcronym() != null) builder.addAllAcronym(LocalizedStringDtos.asDtos(study.getAcronym()));
    if(study.getInvestigators() != null) {
      builder.addAllInvestigators(
          study.getInvestigators().stream().map(ContactDtos::asDto).collect(Collectors.<ContactDto>toList()));
    }
    if(study.getContacts() != null) {
      builder.addAllContacts(
          study.getContacts().stream().map(ContactDtos::asDto).collect(Collectors.<ContactDto>toList()));
    }
    if(!isNullOrEmpty(study.getWebsite())) builder.setWebsite(study.getWebsite());

    //TODO continue

    return builder.build();
  }

  @NotNull
  static Study fromDto(@NotNull Mica.StudyDtoOrBuilder dto) {
    Study study = new Study();
    study.setId(dto.getId());
    study.setName(LocalizedStringDtos.fromDto(dto.getNameList()));
    study.setAcronym(LocalizedStringDtos.fromDto(dto.getAcronymList()));
    study.setInvestigators(
        dto.getInvestigatorsList().stream().map(ContactDtos::fromDto).collect(Collectors.<Contact>toList()));
    study.setContacts(dto.getContactsList().stream().map(ContactDtos::fromDto).collect(Collectors.<Contact>toList()));
    study.setObjectives(LocalizedStringDtos.fromDto(dto.getObjectivesList()));
    if(dto.hasWebsite()) study.setWebsite(dto.getWebsite());

    //TODO continue

    return study;
  }

}
