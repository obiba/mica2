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
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.JSONUtils;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.support.HarmonizedDatasetHelper;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.jsonwebtoken.lang.Assert;

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
  private DatasetDtos datasetDtos;

  @Inject
  private AttachmentDtos attachmentDtos;

  @Inject
  private PermissionsDtos permissionsDtos;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private IndividualStudyService individualStudyService;

  @Inject
  private HarmonizationStudyService harmonizationStudyService;

  @NotNull
  Mica.StudyDto asDto(@NotNull Study study, boolean asDraft) {
    Mica.StudyDto.Builder builder = asDtoBuilder(study, asDraft);
    builder.setExtension(Mica.CollectionStudyDto.type, Mica.CollectionStudyDto.newBuilder().build());
    builder.setPublished(individualStudyService.isPublished(study.getId()));

    return builder.build();
  }

  @NotNull
  Mica.StudyDto asDto(@NotNull HarmonizationStudy study, boolean asDraft, List<HarmonizationDataset> datasets) {
    Mica.HarmonizationStudyDto.Builder hStudyBuilder = Mica.HarmonizationStudyDto.newBuilder();
    HarmonizedDatasetHelper.TablesMerger tableMerger = HarmonizedDatasetHelper.newTablesMerger(datasets);
    tableMerger.getStudyTables()
      .stream().filter(studyTable -> datasetDtos.isStudyTablePermitted(asDraft, "individual", studyTable.getStudyId()))
      .forEach(st -> hStudyBuilder.addStudyTables(datasetDtos.asDto(st, true)));

    tableMerger.getHarmonizationStudyTables()
      .stream().filter(studyTable -> datasetDtos.isStudyTablePermitted(asDraft, "harmonization", studyTable.getStudyId()))
      .forEach(st -> hStudyBuilder.addHarmonizationTables(datasetDtos.asDto(st, true)));

    Mica.StudyDto.Builder builder = asDtoBuilder(study, asDraft);
    builder.setExtension(Mica.HarmonizationStudyDto.type, hStudyBuilder.build());
    builder.setPublished(harmonizationStudyService.isPublished(study.getId()));

    return builder.build();
  }

  @NotNull
  Mica.StudyDto asDto(@NotNull HarmonizationStudy study, boolean asDraft) {
    Mica.StudyDto.Builder builder = asDtoBuilder(study, asDraft);
    builder.setExtension(Mica.HarmonizationStudyDto.type, Mica.HarmonizationStudyDto.newBuilder().build());
    builder.setPublished(harmonizationStudyService.isPublished(study.getId()));

    return builder.build();
  }

  private Mica.StudyDto.Builder asDtoBuilder(@NotNull BaseStudy study, boolean asDraft) {
    Mica.StudyDto.Builder builder = Mica.StudyDto.newBuilder();
    builder.setId(study.getId()) //
      .addAllName(localizedStringDtos.asDto(study.getName())) //
      .addAllObjectives(localizedStringDtos.asDto(study.getObjectives()));

    if(study.hasModel()) builder.setContent(JSONUtils.toJSON(study.getModel()));

    if(asDraft) {
      builder.setTimestamps(TimestampsDtos.asDto(study));
    }

    builder.setStudyResourcePath(study.getResourcePath());
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

    return builder;
  }

  @NotNull
  BaseStudy fromDto(@NotNull Mica.StudyDtoOrBuilder dto) {
    Assert.isTrue(dto.hasExtension(Mica.CollectionStudyDto.type)
      || dto.hasExtension(Mica.HarmonizationStudyDto.type), "StudyDto must of a type extension.");

    BaseStudy study = dto.hasExtension(Mica.CollectionStudyDto.type) ? new Study() : new HarmonizationStudy();

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

    return study;
  }
}
