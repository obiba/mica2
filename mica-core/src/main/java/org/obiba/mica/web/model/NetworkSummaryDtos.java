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

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
class NetworkSummaryDtos {

  private static final Logger log = LoggerFactory.getLogger(NetworkSummaryDtos.class);

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private NetworkService networkService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private PermissionsDtos permissionsDtos;

  @Inject
  private EntityStateDtos entityStateDtos;

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @NotNull
  public Mica.NetworkSummaryDto asDto(@NotNull String id, boolean asDraft) {
    Network network = networkService.findById(id);
    return asDto(network, asDraft);
  }

  @NotNull
  public Mica.NetworkSummaryDto asDto(@NotNull Network network, boolean asDraft) {
    Mica.NetworkSummaryDto.Builder builder = Mica.NetworkSummaryDto.newBuilder();
    NetworkState networkState = networkService.getEntityState(network.getId());

    builder
      .setId(network.getId())
      .addAllAcronym(localizedStringDtos.asDto(network.getAcronym()))
      .addAllName(localizedStringDtos.asDto(network.getName()))
      .setPublished(networkState.isPublished());

    Mica.PermissionsDto permissionsDto = permissionsDtos.asDto(network);

    if(asDraft) {
      builder.setTimestamps(TimestampsDtos.asDto(network)) //
        .setPublished(networkState.isPublished()) //
        .setState(entityStateDtos.asDto(networkState).setPermissions(permissionsDto).build());
    }

    builder.setPermissions(permissionsDto);

    network.getStudyIds().stream()
      .filter(sId -> asDraft && subjectAclService.isPermitted("/draft/individual-study", "VIEW", sId)
        || subjectAclService.isAccessible("/individual-study", sId))
      .forEach(sId -> {
        try {
          builder.addStudyIds(sId);
        } catch(NoSuchEntityException e) {
          log.warn("Study not found in network {}: {}", network.getId(), sId);
          // ignore
        }
      });

    network.getNetworkIds().stream()
      .filter(nId -> asDraft && subjectAclService.isPermitted("/draft/network", "VIEW", nId)
        || subjectAclService.isAccessible("/network", nId))
      .forEach(nId -> {
        try {
          builder.addNetworkIds(nId);
        } catch(NoSuchEntityException e) {
          log.warn("Network not found in network {}: {}", network.getId(), nId);
          // ignore
        }
      });

    return builder.build();
  }

  @NotNull
  Mica.NetworkSummaryDto asDto(String networkId) {
    NetworkState networkState = networkService.getEntityState(networkId);

    if(networkState.isPublished()) {
      Network network = publishedNetworkService.findById(networkId);
      if(network != null) return asDto(network, false);
    }

    return asDto(networkId, true);
  }

  @NotNull
  public Mica.NetworkSummaryDto.Builder asDtoBuilder(@NotNull String id, boolean asDraft) {
    Network network = networkService.findById(id);

    return asDto(network, asDraft).toBuilder();
  }
}
