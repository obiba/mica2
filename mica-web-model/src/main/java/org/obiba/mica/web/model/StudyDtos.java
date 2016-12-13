/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
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
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.study.domain.Study;
import org.springframework.stereotype.Component;

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
  private AttributeDtos attributeDtos;

  @Inject
  private PermissionsDtos permissionsDtos;

  @Inject
  private MicaConfigService micaConfigService;

  @NotNull
  Mica.StudyDto asDto(@NotNull Study study, boolean asDraft) {
    Mica.StudyDto.Builder builder = Mica.StudyDto.newBuilder();
    builder.setId(study.getId()) //
      .addAllName(localizedStringDtos.asDto(study.getName())) //
      .addAllObjectives(localizedStringDtos.asDto(study.getObjectives()));

    if(study.hasModel()) builder.setContent(JSONUtils.toJSON(study.getModel()));

    if(asDraft) {
      builder.setTimestamps(TimestampsDtos.asDto(study));
    }

    builder.setPermissions(permissionsDtos.asDto(study));

    if(study.getLogo() != null) builder.setLogo(attachmentDtos.asDto(study.getLogo()));

    if(study.getAcronym() != null) builder.addAllAcronym(localizedStringDtos.asDto(study.getAcronym()));

    List<String> roles = micaConfigService.getConfig().getRoles();

    if(study.getMemberships() != null) {
      List<Mica.MembershipsDto> memberships = study.getMemberships().entrySet().stream()
        .filter(e -> roles.contains(e.getKey())).map(e -> //
          Mica.MembershipsDto.newBuilder() //
            .setRole(e.getKey()).addAllMembers(e.getValue().stream().map(m -> //
            personDtos.asDto(m.getPerson(), asDraft)).collect(toList())).build()) //
        .collect(toList());

      builder.addAllMemberships(memberships);
    }

    if(!isNullOrEmpty(study.getOpal())) builder.setOpal(study.getOpal());

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
    if(dto.getNameCount() > 0) study.setName(localizedStringDtos.fromDto(dto.getNameList()));
    if(dto.getAcronymCount() > 0) study.setAcronym(localizedStringDtos.fromDto(dto.getAcronymList()));
    if(dto.hasLogo()) study.setLogo(attachmentDtos.fromDto(dto.getLogo()));
    if(dto.hasTimestamps()) TimestampsDtos.fromDto(dto.getTimestamps(), study);
    if(dto.getObjectivesCount() > 0) study.setObjectives(localizedStringDtos.fromDto(dto.getObjectivesList()));
    if(dto.hasOpal()) study.setOpal(dto.getOpal());

    if(dto.getMembershipsCount() > 0) {
      Map<String, List<Membership>> memberships = Maps.newHashMap();
      dto.getMembershipsList().forEach(e -> memberships.put(e.getRole(),
        e.getMembersList().stream().map(p -> new Membership(personDtos.fromDto(p), e.getRole())).collect(toList())));
      study.setMemberships(memberships);
    }

    if (dto.getPopulationsCount() > 0) {
      study.setPopulations(dto.getPopulationsList().stream().map(populationDtos::fromDto)
        .collect(Collectors.toCollection(TreeSet<org.obiba.mica.study.domain.Population>::new)));
    }

    if (dto.hasContent() && !Strings.isNullOrEmpty(dto.getContent()))
      study.setModel(JSONUtils.toMap(dto.getContent()));
    else
      study.setModel(new HashMap<>());

    if (dto.hasMaelstromAuthorization())
      study.getModel().put("maelstromAuthorization", AuthorizationDtos.fromDto(dto.getMaelstromAuthorization()));
    if (dto.hasSpecificAuthorization())
      study.getModel().put("specificAuthorization", AuthorizationDtos.fromDto(dto.getSpecificAuthorization()));

    return study;
  }
}
