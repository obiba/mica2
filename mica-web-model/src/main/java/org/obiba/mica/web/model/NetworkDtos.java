package org.obiba.mica.web.model;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import jersey.repackaged.com.google.common.collect.Lists;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;
import static org.obiba.mica.web.model.Mica.PersonDto;

@Component
class NetworkDtos {

  private static final Logger log = LoggerFactory.getLogger(NetworkDtos.class);

  @Inject
  private PersonDtos personDtos;

  @Inject
  private EntityStateDtos entityStateDtos;

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttachmentDtos attachmentDtos;

  @Inject
  private StudySummaryDtos studySummaryDtos;

  @Inject
  private PermissionsDtos permissionsDtos;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private PublishedDatasetVariableService datasetVariableService;

  @Inject
  private AttributeDtos attributeDtos;

  @Inject
  private NetworkService networkService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private MicaConfigService micaConfigService;

  @NotNull
  Mica.NetworkDto.Builder asDtoBuilder(@NotNull Network network, boolean asDraft) {
    Mica.NetworkDto.Builder builder = Mica.NetworkDto.newBuilder();
    NetworkState networkState = networkService.getEntityState(network.getId());

    builder.setId(network.getId()) //
      .addAllName(localizedStringDtos.asDto(network.getName())) //
      .addAllDescription(localizedStringDtos.asDto(network.getDescription())) //
      .addAllAcronym(localizedStringDtos.asDto(network.getAcronym())) //
      .addAllInfo(localizedStringDtos.asDto(network.getInfos()));

    if(asDraft) {
      builder.setTimestamps(TimestampsDtos.asDto(network)) //
        .setPublished(networkState.isPublished()) //
        .setExtension(Mica.EntityStateDto.state,
          entityStateDtos.asDto(networkState).setPermissions(permissionsDtos.asDto(network)).build());
    }

    List<String> roles = micaConfigService.getConfig().getRoles();

    if(network.getInvestigators() != null && roles.contains(Membership.INVESTIGATOR)) {
      builder.addAllInvestigators(network.getInvestigators().stream().map(p -> personDtos.asDto(p, asDraft))
        .collect(Collectors.<PersonDto>toList()));
    }

    if(network.getContacts() != null && roles.contains(Membership.CONTACT)) {
      builder.addAllContacts(
        network.getContacts().stream().map(p -> personDtos.asDto(p, asDraft)).collect(Collectors.<PersonDto>toList()));
    }

    if(network.getMemberships() != null) {
      List<Mica.MembershipsDto> memberships = network.getMemberships().entrySet().stream()
        .filter(e -> roles.contains(e.getKey())).map(e -> Mica.MembershipsDto.newBuilder().setRole(e.getKey())
          .addAllMembers(e.getValue().stream().map(m -> personDtos.asDto(m.getPerson(), asDraft)).collect(toList()))
          .build()).collect(toList());

      builder.addAllMemberships(memberships);
    }

    if(!isNullOrEmpty(network.getWebsite())) builder.setWebsite(network.getWebsite());

    List<Study> publishedStudies = publishedStudyService.findByIds(network.getStudyIds());
    Set<String> publishedStudyIds = publishedStudies.stream().map(AbstractGitPersistable::getId)
      .collect(Collectors.toSet());
    Sets.SetView<String> unpublishedStudyIds = Sets.difference(ImmutableSet.copyOf(
      network.getStudyIds().stream().filter(sId -> asDraft || subjectAclService.isAccessible("/study", sId))
        .collect(toList())), publishedStudyIds);

    if(!publishedStudies.isEmpty()) {
      Map<String, Long> datasetVariableCounts = datasetVariableService
        .getCountByStudyIds(Lists.newArrayList(publishedStudyIds));

      publishedStudies.forEach(study -> {
        builder.addStudyIds(study.getId());
        builder.addStudySummaries(studySummaryDtos.asDtoBuilder(study, true, datasetVariableCounts.get(study.getId())));
      });
    }

    unpublishedStudyIds.forEach(studyId -> {
      builder.addStudyIds(studyId);
      builder.addStudySummaries(studySummaryDtos.asDto(studyId));
    });

    if(network.getMaelstromAuthorization() != null) {
      builder.setMaelstromAuthorization(AuthorizationDtos.asDto(network.getMaelstromAuthorization()));
    }

    if(network.getLogo() != null) {
      builder.setLogo(attachmentDtos.asDto(network.getLogo()));
    }

    if(network.getAttributes() != null) {
      network.getAttributes().asAttributeList()
        .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }

    return builder;
  }

  /**
   * Get the dto of the network.
   *
   * @param network
   * @param asDraft
   * @return
   */
  @NotNull
  Mica.NetworkDto asDto(@NotNull Network network, boolean asDraft) {
    return asDtoBuilder(network, asDraft).build();
  }

  @NotNull
  Network fromDto(@NotNull Mica.NetworkDtoOrBuilder dto) {
    Network network = new Network();

    if(dto.hasId()) {
      network.setId(dto.getId());
    }

    network.setName(localizedStringDtos.fromDto(dto.getNameList()));
    network.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    network.setAcronym(localizedStringDtos.fromDto(dto.getAcronymList()));
    network.setInfos(localizedStringDtos.fromDto(dto.getInfoList()));

    if(dto.getMembershipsCount() > 0) {
      Map<String, List<Membership>> memberships = Maps.newHashMap();
      dto.getMembershipsList().forEach(e -> memberships.put(e.getRole(),
        e.getMembersList().stream().map(p -> new Membership(personDtos.fromDto(p), e.getRole())).collect(toList())));
      network.setMemberships(memberships);
    } else { //backwards compatibility
      network.setInvestigators(
        dto.getInvestigatorsList().stream().map(personDtos::fromDto).collect(Collectors.<Person>toList()));
      network.setContacts(dto.getContactsList().stream().map(personDtos::fromDto).collect(Collectors.<Person>toList()));
    }

    if(dto.hasWebsite()) network.setWebsite(dto.getWebsite());

    if(dto.getStudyIdsCount() > 0) {
      dto.getStudyIdsList().forEach(network::addStudyId);
    }

    if(dto.getAttachmentsCount() > 0) {
      dto.getAttachmentsList().stream().filter(a -> a.getJustUploaded()).findFirst()
        .ifPresent(a -> network.setLogo(attachmentDtos.fromDto(a)));
    }

    if(dto.hasMaelstromAuthorization())
      network.setMaelstromAuthorization(AuthorizationDtos.fromDto(dto.getMaelstromAuthorization()));

    if(dto.hasLogo()) {
      network.setLogo(attachmentDtos.fromDto(dto.getLogo()));
    }

    if(dto.getAttributesCount() > 0) {
      dto.getAttributesList().forEach(attributeDto -> network.addAttribute(attributeDtos.fromDto(attributeDto)));
    }
    return network;
  }

}
