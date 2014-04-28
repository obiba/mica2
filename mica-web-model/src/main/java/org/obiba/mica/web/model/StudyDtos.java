package org.obiba.mica.web.model;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Attachment;
import org.obiba.mica.domain.Contact;
import org.obiba.mica.domain.Population;
import org.obiba.mica.domain.Study;
import org.obiba.mica.domain.Timestamped;
import org.springframework.stereotype.Component;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.obiba.mica.web.model.Mica.ContactDto;

@Component
class StudyDtos {

  @Inject
  private ContactDtos contactDtos;

  @NotNull
  Mica.StudyDto asDto(@NotNull Study study, Timestamped studyState) {
    Mica.StudyDto.Builder builder = Mica.StudyDto.newBuilder();
    builder.setId(study.getId()) //
        .setTimestamps(TimestampsDtos.asDto(studyState)) //
        .addAllName(LocalizedStringDtos.asDto(study.getName())) //
        .addAllObjectives(LocalizedStringDtos.asDto(study.getObjectives()));

    if (study.getStartYear() != null) builder.setStartYear(study.getStartYear());
    study.getAccess().stream().forEach(access -> builder.addAccess(access));
    if (study.getOtherAccess() != null) builder.addAllOtherAccess(LocalizedStringDtos.asDto(study.getOtherAccess()));
    if (study.getMarkerPaper() != null) builder.setMarkerPaper(study.getMarkerPaper());
    if (study.getPubmedId() != null) builder.setPubmedId(study.getPubmedId());

    if(study.getAcronym() != null) builder.addAllAcronym(LocalizedStringDtos.asDto(study.getAcronym()));
    if(study.getInvestigators().size() > 0) {
      builder.addAllInvestigators(
          study.getInvestigators().stream().map(contactDtos::asDto).collect(Collectors.<ContactDto>toList()));
    }
    if(study.getContacts().size() > 0) {
      builder.addAllContacts(
          study.getContacts().stream().map(contactDtos::asDto).collect(Collectors.<ContactDto>toList()));
    }
    if(!isNullOrEmpty(study.getWebsite())) builder.setWebsite(study.getWebsite());
    if(study.getSpecificAuthorization() != null)
      builder.setSpecificAuthorization(AuthorizationDtos.asDto(study.getSpecificAuthorization()));
    if(study.getMaelstromAuthorization() != null)
      builder.setMaelstromAuthorization(AuthorizationDtos.asDto(study.getMaelstromAuthorization()));
    if (study.getMethods() != null) builder.setMethods(asDto(study.getMethods()));
    if(study.getNumberOfParticipants() != null) {
      builder.setNumberOfParticipants(NumberOfParticipantsDtos.asDto(study.getNumberOfParticipants()));
    }
    if (study.getAttachments().size() > 0) {
      builder.addAllAttachments(
          study.getAttachments().stream().map(AttachmentDtos::asDto).collect(Collectors.<Mica.AttachmentDto>toList()));
    }

    study.getPopulations().stream().forEach(population -> builder.addPopulations(PopulationDtos.asDto(population)));
    //TODO continue

    return builder.build();
  }

  @NotNull
  Study fromDto(@NotNull Mica.StudyDtoOrBuilder dto) {
    Study study = new Study();
    study.setId(dto.getId());
    if (dto.hasStartYear()) study.setStartYear(dto.getStartYear());
    if (dto.getAccessCount() > 0) study.setAccess(dto.getAccessList());
    if (dto.getOtherAccessCount() > 0) study.setOtherAccess(LocalizedStringDtos.fromDto(dto.getOtherAccessList()));
    if (dto.hasMarkerPaper()) study.setMarkerPaper(dto.getMarkerPaper());
    if (dto.hasPubmedId()) study.setPubmedId(dto.getPubmedId());
    if (dto.getNameCount() > 0) study.setName(LocalizedStringDtos.fromDto(dto.getNameList()));
    if (dto.getAcronymCount() > 0) study.setAcronym(LocalizedStringDtos.fromDto(dto.getAcronymList()));
    study.setInvestigators(
        dto.getInvestigatorsList().stream().map(contactDtos::fromDto).collect(Collectors.<Contact>toList()));
    study.setContacts(dto.getContactsList().stream().map(contactDtos::fromDto).collect(Collectors.<Contact>toList()));
    if (dto.getObjectivesCount() > 0) study.setObjectives(LocalizedStringDtos.fromDto(dto.getObjectivesList()));
    if(dto.hasWebsite()) study.setWebsite(dto.getWebsite());
    if(dto.hasSpecificAuthorization()) {
      study.setSpecificAuthorization(AuthorizationDtos.fromDto(dto.getSpecificAuthorization()));
    }
    if(dto.hasMaelstromAuthorization()) {
      study.setMaelstromAuthorization(AuthorizationDtos.fromDto(dto.getMaelstromAuthorization()));
    }
    if (dto.hasMethods()) study.setMethods(fromDto(dto.getMethods()));
    if(dto.hasNumberOfParticipants()) {
      study.setNumberOfParticipants(NumberOfParticipantsDtos.fromDto(dto.getNumberOfParticipants()));
    }
    study.setAttachments(
        dto.getAttachmentsList().stream().map(AttachmentDtos::fromDto).collect(Collectors.<Attachment>toList()));
    study.setPopulations(
        dto.getPopulationsList().stream().map(PopulationDtos::fromDto).collect(Collectors.<Population>toList()));
    //TODO continue

    return study;
  }

  @NotNull
  private Mica.StudyDto.StudyMethodsDto asDto(@NotNull Study.StudyMethods studyMethods) {
    Mica.StudyDto.StudyMethodsDto.Builder builder = Mica.StudyDto.StudyMethodsDto.newBuilder();
    studyMethods.getDesigns().forEach(design -> builder.addDesigns(design));
    if(studyMethods.getOtherDesign() != null) {
      builder.addAllOtherDesign(LocalizedStringDtos.asDto(studyMethods.getOtherDesign()));
    }
    if(studyMethods.getFollowUpInfo() != null) {
      builder.addAllFollowUpInfo(LocalizedStringDtos.asDto(studyMethods.getFollowUpInfo()));
    }
    studyMethods.getRecruitments().forEach(recruitment -> builder.addRecruitments(recruitment));
    if(studyMethods.getOtherRecruitment() != null) {
      builder.addAllOtherRecruitment(LocalizedStringDtos.asDto(studyMethods.getOtherRecruitment()));
    }
    if(studyMethods.getInfo() != null) {
      builder.addAllInfo(LocalizedStringDtos.asDto(studyMethods.getInfo()));
    }
    return builder.build();
  }

  @NotNull
  private Study.StudyMethods fromDto(@NotNull Mica.StudyDto.StudyMethodsDto dto) {
    Study.StudyMethods studyMethods = new Study.StudyMethods();
    if (dto.getDesignsCount() > 0) studyMethods.setDesigns(dto.getDesignsList());
    if(dto.getOtherDesignCount() > 0){
      studyMethods.setOtherDesign(LocalizedStringDtos.fromDto(dto.getOtherDesignList()));
    }
    if(dto.getFollowUpInfoCount() > 0) {
      studyMethods.setFollowUpInfo(LocalizedStringDtos.fromDto(dto.getFollowUpInfoList()));
    }
    if(dto.getRecruitmentsCount() > 0) {
      studyMethods.setRecruitments(dto.getRecruitmentsList());
    }
    if(dto.getOtherRecruitmentCount() > 0) {
      studyMethods.setOtherRecruitment(LocalizedStringDtos.fromDto(dto.getOtherRecruitmentList()));
    }
    if(dto.getInfoCount() > 0) {
      studyMethods.setInfo(LocalizedStringDtos.fromDto(dto.getInfoList()));
    }

    return studyMethods;
  }
}
