/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import jersey.repackaged.com.google.common.collect.Lists;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Mica.MembershipSortOrderDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

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
  private NetworkService networkService;

  @Inject
  private PersonService personService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private NetworkSummaryDtos networkSummaryDtos;

  @NotNull
  Mica.NetworkDto.Builder asDtoBuilder(@NotNull Network network, boolean asDraft) {
    Mica.NetworkDto.Builder builder = asDtoBuilderInternal(network, asDraft);
    List<String> roles = micaConfigService.getConfig().getRoles();

    if (network.getMembershipSortOrder() != null) {
      network.getMembershipSortOrder().forEach((role, ids) -> builder.addMembershipSortOrder(MembershipSortOrderDto.newBuilder().setRole(role).addAllPersonIds(ids).build()));
    }

    Map<String, List<Membership>> networkMembershipMap = personService.getNetworkMembershipMap(network.getId());

    List<Mica.MembershipsDto> memberships = personService.setMembershipOrder(network.getMembershipSortOrder(), networkMembershipMap)
      .entrySet().stream()
      .filter(e -> roles.contains(e.getKey())).map(e -> Mica.MembershipsDto.newBuilder().setRole(e.getKey())
        .addAllMembers(e.getValue().stream().map(m -> personDtos.asDto(m.getPerson(), asDraft)).collect(toList()))
        .build()).collect(toList());

    builder.addAllMemberships(memberships);

    return builder;
  }

  @NotNull
  Mica.NetworkDto.Builder asDtoBuilderForSearchListing(@NotNull Network network, boolean asDraft) {
    return asDtoBuilderInternal(network, asDraft);
  }

  private Mica.NetworkDto.Builder asDtoBuilderInternal(@NotNull Network network, boolean asDraft) {
    Mica.NetworkDto.Builder builder = Mica.NetworkDto.newBuilder();

    if(network.hasModel()) builder.setContent(JSONUtils.toJSON(network.getModel()));

    builder.setId(network.getId()) //
      .addAllName(localizedStringDtos.asDto(network.getName())) //
      .addAllDescription(localizedStringDtos.asDto(network.getDescription())) //
      .addAllAcronym(localizedStringDtos.asDto(network.getAcronym()));

    NetworkState networkState = networkService.getEntityState(network.getId());
    builder.setPublished(networkState.isPublished());
    if(asDraft) {
      Mica.PermissionsDto permissionsDto = permissionsDtos.asDto(network);

      builder.setTimestamps(TimestampsDtos.asDto(network)) //
        .setPublished(networkState.isPublished()) //
        .setExtension(Mica.EntityStateDto.state,
          entityStateDtos.asDto(networkState).setPermissions(permissionsDto).build());

      builder.setPermissions(permissionsDto);
    }

    if(network.getLogo() != null) {
      builder.setLogo(attachmentDtos.asDto(network.getLogo()));
    }

    List<BaseStudy> publishedStudies = publishedStudyService.findByIds(network.getStudyIds());
    Set<String> publishedStudyIds = publishedStudies.stream().map(AbstractGitPersistable::getId)
      .collect(Collectors.toSet());
    Sets.SetView<String> unpublishedStudyIds = Sets.difference(ImmutableSet.copyOf(
      network.getStudyIds().stream()
        .filter(sId -> asDraft && subjectAclService.isPermitted("/draft/individual-study", "VIEW", sId)
          || subjectAclService.isAccessible("/individual-study", sId))
        .collect(toList())), publishedStudyIds);

    if(!publishedStudies.isEmpty()) {
      Map<String, Long> datasetVariableCounts = asDraft ? null :
        datasetVariableService.getCountByStudyIds(Lists.newArrayList(publishedStudyIds));

      publishedStudies.forEach(study -> {
        builder.addStudyIds(study.getId());
        builder.addStudySummaries(
          studySummaryDtos.asDtoBuilder(study, true,
            datasetVariableCounts == null ? 0 : datasetVariableCounts.get(study.getId())));
      });
    }

    unpublishedStudyIds.forEach(studyId -> {
      try {
        builder.addStudySummaries(studySummaryDtos.asDto(studyId));
        builder.addStudyIds(studyId);
      } catch(NoSuchEntityException e) {
        log.warn("Study not found in network {}: {}", network.getId(), studyId);
        // ignore
      }
    });

    network.getNetworkIds().stream()
      .filter(nId -> asDraft && subjectAclService.isPermitted("/draft/network", "VIEW", nId)
        || subjectAclService.isAccessible("/network", nId))
      .forEach(nId -> {
        try {
          builder.addNetworkSummaries(networkSummaryDtos.asDtoBuilder(nId, asDraft));
          builder.addNetworkIds(nId);
        } catch(NoSuchEntityException e) {
          log.warn("Network not found in network {}: {}", network.getId(), nId);
          // ignore
        }
      });

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

    if(dto.getStudyIdsCount() > 0) {
      dto.getStudyIdsList().forEach(network::addStudyId);
    }

    if(dto.getAttachmentsCount() > 0) {
      dto.getAttachmentsList().stream().filter(Mica.AttachmentDto::getJustUploaded).findFirst()
        .ifPresent(a -> network.setLogo(attachmentDtos.fromDto(a)));
    }

    if(dto.hasLogo()) {
      network.setLogo(attachmentDtos.fromDto(dto.getLogo()));
    }

    if(dto.getNetworkIdsCount() > 0) {
      network.setNetworkIds(Lists.newArrayList(Sets.newHashSet(dto.getNetworkIdsList())));
    }

    if (dto.getMembershipSortOrderCount() > 0) {
      Map<String, List<String>> membershipSortOrder = new HashMap<>();

      dto.getMembershipSortOrderList().forEach(membership -> {
        membershipSortOrder.put(membership.getRole(), membership.getPersonIdsList());
      });

      network.setMembershipSortOrder(membershipSortOrder);
    }

    if (dto.hasContent() && !Strings.isNullOrEmpty(dto.getContent()))
      network.setModel(JSONUtils.toMap(dto.getContent()));
    else
      network.setModel(new HashMap<>());

    return network;
  }
}
