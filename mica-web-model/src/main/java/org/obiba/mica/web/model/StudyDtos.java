package org.obiba.mica.web.model;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import static com.google.common.base.Strings.isNullOrEmpty;

import static java.util.stream.Collectors.toList;

@Component
@SuppressWarnings("OverlyLongMethod")
class StudyDtos {

  @Inject
  private PersonDtos personDtos;

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private PopulationDtos populationDtos;

  @Inject
  private AttachmentDtos attachmentDtos;

  @Inject
  private NumberOfParticipantsDtos numberOfParticipantsDtos;

  @Inject
  private AttributeDtos attributeDtos;

  @Inject
  private PermissionsDtos permissionsDtos;

  @NotNull
  Mica.StudyDto asDto(@NotNull Study study, boolean asDraft) {
    Mica.StudyDto.Builder builder = Mica.StudyDto.newBuilder();
    builder.setId(study.getId()) //
      .addAllName(localizedStringDtos.asDto(study.getName())) //
      .addAllObjectives(localizedStringDtos.asDto(study.getObjectives()));

    if(asDraft) {
      builder.setTimestamps(TimestampsDtos.asDto(study));
      builder.setPermissions(permissionsDtos.asDto(study));
    }

    if(study.getLogo() != null) builder.setLogo(attachmentDtos.asDto(study.getLogo()));

    if(study.getStart() != null) builder.setStartYear(study.getStart());
    if(study.getEnd() != null) builder.setEndYear(study.getEnd());
    if(study.getAccess() != null) {
      study.getAccess().forEach(builder::addAccess);
    }
    if(study.getOtherAccess() != null) builder.addAllOtherAccess(localizedStringDtos.asDto(study.getOtherAccess()));
    if(study.getMarkerPaper() != null) builder.setMarkerPaper(study.getMarkerPaper());
    if(study.getPubmedId() != null) builder.setPubmedId(study.getPubmedId());

    if(study.getAcronym() != null) builder.addAllAcronym(localizedStringDtos.asDto(study.getAcronym()));
    if(study.getInvestigators() != null) {
      builder.addAllInvestigators(
        study.getInvestigators().stream().map(p -> personDtos.asDto(p, asDraft)).collect(toList()));
    }
    if(study.getContacts() != null) {
      builder
        .addAllContacts(study.getContacts().stream().map(p -> personDtos.asDto(p, asDraft)).collect(toList()));
    }

    if(study.getMemberships() != null) {
      List<Mica.MembershipsDto> memberships = study.getMemberships().entrySet().stream().map(
        e -> Mica.MembershipsDto.newBuilder().setRole(e.getKey())
          .addAllMembers(e.getValue().stream().map(m -> personDtos.asDto(m.getPerson(), asDraft)).collect(toList()))
          .build()).collect(toList());

      builder.addAllMemberships(memberships);
    }

    if(!isNullOrEmpty(study.getWebsite())) builder.setWebsite(study.getWebsite());
    if(!isNullOrEmpty(study.getOpal())) builder.setOpal(study.getOpal());
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
    if(study.getInfo() != null) {
      builder.addAllInfo(localizedStringDtos.asDto(study.getInfo()));
    }
    if(study.getPopulations() != null) {
      study.getPopulations().forEach(population -> builder.addPopulations(populationDtos.asDto(population)));
    }
    if(study.getAttributes() != null) {
      study.getAttributes().asAttributeList()
        .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }
    return builder.build();
  }

  @NotNull
  Study fromDto(@NotNull Mica.StudyDtoOrBuilder dto) {
    Study study = new Study();
    if(dto.hasId()) study.setId(dto.getId());
    if(dto.hasTimestamps()) TimestampsDtos.fromDto(dto.getTimestamps(), study);
    if(dto.hasStartYear()) study.setStart(dto.getStartYear());
    if(dto.hasEndYear()) study.setEnd(dto.getEndYear());
    if(dto.getAccessCount() > 0) study.setAccess(dto.getAccessList());
    if(dto.getOtherAccessCount() > 0) study.setOtherAccess(localizedStringDtos.fromDto(dto.getOtherAccessList()));
    if(dto.hasMarkerPaper()) study.setMarkerPaper(dto.getMarkerPaper());
    if(dto.hasPubmedId()) study.setPubmedId(dto.getPubmedId());
    if(dto.getNameCount() > 0) study.setName(localizedStringDtos.fromDto(dto.getNameList()));
    if(dto.getAcronymCount() > 0) study.setAcronym(localizedStringDtos.fromDto(dto.getAcronymList()));
    if(dto.hasLogo()) study.setLogo(attachmentDtos.fromDto(dto.getLogo()));

    if(dto.getMembershipsCount() > 0) {
      Map<String, List<Membership>> memberships = Maps.newHashMap();
      dto.getMembershipsList().forEach(e -> memberships.put(e.getRole(),
        e.getMembersList().stream().map(p -> new Membership(personDtos.fromDto(p), e.getRole())).collect(toList())));
      study.setMemberships(memberships);
    } else {
      if(dto.getContactsCount() > 0) {
        study.setContacts(dto.getContactsList().stream().map(personDtos::fromDto).collect(Collectors.<Person>toList()));
      }
      if(dto.getInvestigatorsCount() > 0) {
        study.setInvestigators(
          dto.getInvestigatorsList().stream().map(personDtos::fromDto).collect(Collectors.<Person>toList()));
      }
    }

    if(dto.getObjectivesCount() > 0) study.setObjectives(localizedStringDtos.fromDto(dto.getObjectivesList()));
    if(dto.hasWebsite()) study.setWebsite(dto.getWebsite());
    if(dto.hasOpal()) study.setOpal(dto.getOpal());
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
    if(dto.getInfoCount() > 0) study.setInfo(localizedStringDtos.fromDto(dto.getInfoList()));
    if(dto.getPopulationsCount() > 0) {
      study.setPopulations(dto.getPopulationsList().stream().map(populationDtos::fromDto)
        .collect(Collectors.toCollection(TreeSet<Population>::new)));
    }
    if(dto.getAttributesCount() > 0) {
      dto.getAttributesList().forEach(attributeDto -> study.addAttribute(attributeDtos.fromDto(attributeDto)));
    }

    return study;
  }

  @NotNull
  private Mica.StudyDto.StudyMethodsDto asDto(@NotNull Study.StudyMethods studyMethods) {
    Mica.StudyDto.StudyMethodsDto.Builder builder = Mica.StudyDto.StudyMethodsDto.newBuilder();
    if(studyMethods.getDesigns() != null) {
      builder.addAllDesigns(studyMethods.getDesigns());
    }
    if(studyMethods.getOtherDesign() != null && !studyMethods.getOtherDesign().isEmpty()) {
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
