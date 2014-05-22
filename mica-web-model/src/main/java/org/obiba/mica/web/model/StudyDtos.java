package org.obiba.mica.web.model;

import java.time.Year;
import java.util.TreeSet;
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
@SuppressWarnings("OverlyLongMethod")
class StudyDtos {

  @Inject
  private ContactDtos contactDtos;

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private PopulationDtos populationDtos;

  @Inject
  private AttachmentDtos attachmentDtos;

  @Inject
  private NumberOfParticipantsDtos numberOfParticipantsDtos;

  @NotNull
  Mica.StudyDto asDto(@NotNull Study study, Timestamped studyState) {
    Mica.StudyDto.Builder builder = Mica.StudyDto.newBuilder();
    builder.setId(study.getId()) //
        .setTimestamps(TimestampsDtos.asDto(studyState)) //
        .addAllName(localizedStringDtos.asDto(study.getName())) //
        .addAllObjectives(localizedStringDtos.asDto(study.getObjectives()));

    if(study.getStart() != null) builder.setStartYear(study.getStart().getValue());
    if(study.getAccess() != null) {
      study.getAccess().forEach(builder::addAccess);
    }
    if(study.getOtherAccess() != null) builder.addAllOtherAccess(localizedStringDtos.asDto(study.getOtherAccess()));
    if(study.getMarkerPaper() != null) builder.setMarkerPaper(study.getMarkerPaper());
    if(study.getPubmedId() != null) builder.setPubmedId(study.getPubmedId());

    if(study.getAcronym() != null) builder.addAllAcronym(localizedStringDtos.asDto(study.getAcronym()));
    if(study.getInvestigators() != null) {
      builder.addAllInvestigators(
          study.getInvestigators().stream().map(contactDtos::asDto).collect(Collectors.<ContactDto>toList()));
    }
    if(study.getContacts() != null) {
      builder.addAllContacts(
          study.getContacts().stream().map(contactDtos::asDto).collect(Collectors.<ContactDto>toList()));
    }
    if(!isNullOrEmpty(study.getWebsite())) builder.setWebsite(study.getWebsite());
    if(study.getSpecificAuthorization() != null) {
      builder.setSpecificAuthorization(AuthorizationDtos.asDto(study.getSpecificAuthorization()));
    }
    if(study.getMaelstromAuthorization() != null) {
      builder.setMaelstromAuthorization(AuthorizationDtos.asDto(study.getMaelstromAuthorization()));
    }
    if(study.getMethods() != null) builder.setMethods(asDto(study.getMethods()));
    if(study.getNumberOfParticipants() != null) {
      builder.setNumberOfParticipants(numberOfParticipantsDtos.asDto(study.getNumberOfParticipants()));
    }
    if(study.getAttachments() != null) {
      builder.addAllAttachments(
          study.getAttachments().stream().map(attachmentDtos::asDto).collect(Collectors.<Mica.AttachmentDto>toList()));
    }
    if(study.getPopulations() != null) {
      study.getPopulations().forEach(population -> builder.addPopulations(populationDtos.asDto(population)));
    }
    return builder.build();
  }

  @NotNull
  Study fromDto(@NotNull Mica.StudyDtoOrBuilder dto) {
    Study study = new Study();
    if(dto.hasId()) study.setId(dto.getId());
    if(dto.hasStartYear()) study.setStart(Year.of(dto.getStartYear()));
    if(dto.getAccessCount() > 0) study.setAccess(dto.getAccessList());
    if(dto.getOtherAccessCount() > 0) study.setOtherAccess(localizedStringDtos.fromDto(dto.getOtherAccessList()));
    if(dto.hasMarkerPaper()) study.setMarkerPaper(dto.getMarkerPaper());
    if(dto.hasPubmedId()) study.setPubmedId(dto.getPubmedId());
    if(dto.getNameCount() > 0) study.setName(localizedStringDtos.fromDto(dto.getNameList()));
    if(dto.getAcronymCount() > 0) study.setAcronym(localizedStringDtos.fromDto(dto.getAcronymList()));
    if(dto.getInvestigatorsCount() > 0) {
      study.setInvestigators(
          dto.getInvestigatorsList().stream().map(contactDtos::fromDto).collect(Collectors.<Contact>toList()));
    }
    if(dto.getContactsCount() > 0) {
      study.setContacts(dto.getContactsList().stream().map(contactDtos::fromDto).collect(Collectors.<Contact>toList()));
    }
    if(dto.getObjectivesCount() > 0) study.setObjectives(localizedStringDtos.fromDto(dto.getObjectivesList()));
    if(dto.hasWebsite()) study.setWebsite(dto.getWebsite());
    if(dto.hasSpecificAuthorization()) {
      study.setSpecificAuthorization(AuthorizationDtos.fromDto(dto.getSpecificAuthorization()));
    }
    if(dto.hasMaelstromAuthorization()) {
      study.setMaelstromAuthorization(AuthorizationDtos.fromDto(dto.getMaelstromAuthorization()));
    }
    if(dto.hasMethods()) study.setMethods(fromDto(dto.getMethods()));
    if(dto.hasNumberOfParticipants()) {
      study.setNumberOfParticipants(numberOfParticipantsDtos.fromDto(dto.getNumberOfParticipants()));
    }
    if(dto.getAttachmentsCount() > 0) {
      study.setAttachments(
          dto.getAttachmentsList().stream().map(attachmentDtos::fromDto).collect(Collectors.<Attachment>toList()));
    }
    if(dto.getPopulationsCount() > 0) {
      study.setPopulations(dto.getPopulationsList().stream().map(populationDtos::fromDto)
              .collect(Collectors.toCollection(TreeSet<Population>::new))
      );
    }
    return study;
  }

  @NotNull
  private Mica.StudyDto.StudyMethodsDto asDto(@NotNull Study.StudyMethods studyMethods) {
    Mica.StudyDto.StudyMethodsDto.Builder builder = Mica.StudyDto.StudyMethodsDto.newBuilder();
    if(studyMethods.getDesigns() != null) studyMethods.getDesigns().forEach(builder::addDesigns);
    if(studyMethods.getOtherDesign() != null) {
      builder.addAllOtherDesign(localizedStringDtos.asDto(studyMethods.getOtherDesign()));
    }
    if(studyMethods.getFollowUpInfo() != null) {
      builder.addAllFollowUpInfo(localizedStringDtos.asDto(studyMethods.getFollowUpInfo()));
    }
    if(studyMethods.getRecruitments() != null) {
      studyMethods.getRecruitments().forEach(builder::addRecruitments);
    }
    if(studyMethods.getOtherRecruitment() != null) {
      builder.addAllOtherRecruitment(localizedStringDtos.asDto(studyMethods.getOtherRecruitment()));
    }
    if(studyMethods.getInfo() != null) {
      builder.addAllInfo(localizedStringDtos.asDto(studyMethods.getInfo()));
    }
    return builder.build();
  }

  @NotNull
  private Study.StudyMethods fromDto(@NotNull Mica.StudyDto.StudyMethodsDtoOrBuilder dto) {
    Study.StudyMethods studyMethods = new Study.StudyMethods();
    if(dto.getDesignsCount() > 0) studyMethods.setDesigns(dto.getDesignsList());
    if(dto.getOtherDesignCount() > 0) {
      studyMethods.setOtherDesign(localizedStringDtos.fromDto(dto.getOtherDesignList()));
    }
    if(dto.getFollowUpInfoCount() > 0) {
      studyMethods.setFollowUpInfo(localizedStringDtos.fromDto(dto.getFollowUpInfoList()));
    }
    if(dto.getRecruitmentsCount() > 0) {
      studyMethods.setRecruitments(dto.getRecruitmentsList());
    }
    if(dto.getOtherRecruitmentCount() > 0) {
      studyMethods.setOtherRecruitment(localizedStringDtos.fromDto(dto.getOtherRecruitmentList()));
    }
    if(dto.getInfoCount() > 0) {
      studyMethods.setInfo(localizedStringDtos.fromDto(dto.getInfoList()));
    }
    return studyMethods;
  }
}
