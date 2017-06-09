/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
class StudySummaryDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttachmentDtos attachmentDtos;

  @Inject
  private PermissionsDtos permissionsDtos;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private PublishedDatasetVariableService datasetVariableService;

  @Inject
  private StudyService studyService;

  @NotNull
  public Mica.StudySummaryDto.Builder asDtoBuilder(@NotNull Study study) {
    StudyState studyState = studyService.getEntityState(study.getId());

    if(studyState.isPublished()) {
      return asDtoBuilder(study, studyState.isPublished(), datasetVariableService.getCountByStudyId(study.getId()));
    }

    return asDtoBuilder(study, studyState.isPublished(), 0);
  }

  @NotNull
  public Mica.StudySummaryDto.Builder asDtoBuilder(@NotNull Study study, boolean isPublished, long variablesCount) {
    Mica.StudySummaryDto.Builder builder = Mica.StudySummaryDto.newBuilder();
    builder.setPublished(isPublished);

    builder.setId(study.getId()) //
      .setTimestamps(TimestampsDtos.asDto(study)) //
      .addAllName(localizedStringDtos.asDto(study.getName())) //
      .addAllAcronym(localizedStringDtos.asDto(study.getAcronym())) //
      .addAllObjectives(localizedStringDtos.asDto(study.getObjectives()))
      .setVariables(isPublished ? variablesCount : 0);

    if(study.getLogo() != null) builder.setLogo(attachmentDtos.asDto(study.getLogo()));

    if(study.getMethods() != null && study.getMethods().getDesigns() != null) {
      builder.addAllDesigns(study.getMethods().getDesigns());
    } else { // NOTICE: schemaform backwards compatibility
      try {
        Map<String, Object> methods = (Map<String, Object>) study.getModel().get("methods");

        if (methods != null && methods.get("design") != null) {
          builder.addAllDesigns(Lists.newArrayList((String) methods.get("design")));
        }
      } catch (NullPointerException | ClassCastException e) {
      }
    }

    if(study.getNumberOfParticipants() != null && study.getNumberOfParticipants().getParticipant() != null) {
      builder.setTargetNumber(TargetNumberDtos.asDto(study.getNumberOfParticipants().getParticipant()));
    } else { // NOTICE: schemaform backwards compatibility
      try {
        Optional.ofNullable((Map<String, Object>) study.getModel().get("numberOfParticipants")) //
          .flatMap(n -> Optional.ofNullable((Map<String, Object>) n.get("participant"))) //
          .map(p -> Mica.TargetNumberDto.newBuilder().setNoLimit((boolean) p.get("noLimit")).setNumber((int) p.get("number")).build()) //
          .ifPresent(builder::setTargetNumber);
      } catch (NullPointerException | ClassCastException e) {
      }
    }

    Collection<String> countries = new HashSet<>();
    SortedSet<Population> populations = study.getPopulations();

    if(populations != null) {
      if(!study.hasModel()) {
        populations.stream() //
          .filter(population -> population.getSelectionCriteria() != null &&
            population.getSelectionCriteria().getCountriesIso() != null)
          .forEach(population -> countries.addAll(population.getSelectionCriteria().getCountriesIso()));

        List<String> dataSources = Lists.newArrayList();
        populations.stream().filter(population -> population.getAllDataSources() != null)
          .forEach(population -> dataSources.addAll(population.getAllDataSources()));

        if (dataSources.size() > 0) {
          builder.addAllDataSources(dataSources.stream().distinct().collect(toList()));
        }

      } else {
        countries.addAll(extractCountries(populations));

        List<String> dataSources = Lists.newArrayList();
        populations.stream().filter(population -> population.getAllDataSources() != null)
          .forEach(population -> dataSources.addAll(population.getAllDataSources()));

        if (dataSources.size() > 0) {
          builder.addAllDataSources(dataSources.stream().distinct().collect(toList()));
        }
      }

      populations.forEach(population -> builder.addPopulationSummaries(asDto(population)));
    }

    builder.setPermissions(permissionsDtos.asDto(study));

    builder.addAllCountries(countries);

    return builder;
  }

  Set<String> extractCountries(Collection<Population> populations) {

    return populations.stream().filter(Population::hasModel)
      .flatMap(p -> {
         if(p.getModel().get("selectionCriteria") instanceof Map) { //TODO: serialization should not include JsonTypeInfo to avoid this check.
           Map<String, Object> sc = (Map<String, Object>) p.getModel().get("selectionCriteria");
           List<String> countriesIso = sc.containsKey("countriesIso") ? (List<String>)sc.get("countriesIso") : null;
           return countriesIso != null ? countriesIso.stream() : Stream.empty();
         } else {
           return Optional.ofNullable((Population.SelectionCriteria) p.getModel().get("selectionCriteria"))
             .flatMap(sc -> Optional.ofNullable(sc.getCountriesIso().stream()))
             .orElseGet(Stream::empty);
         }
        }
      ).filter(country -> country != null)
      .collect(toSet());
  }

  @NotNull
  Mica.PopulationSummaryDto asDto(@NotNull Population population) {
    Mica.PopulationSummaryDto.Builder builder = Mica.PopulationSummaryDto.newBuilder();

    builder.setId(population.getId()) //
      .addAllName(localizedStringDtos.asDto(population.getName()));

    if(population.getDataCollectionEvents() != null) {
      population.getDataCollectionEvents().forEach(dce -> builder.addDataCollectionEventSummaries(asDto(dce)));
    }

    return builder.build();
  }

  @NotNull
  Mica.DataCollectionEventSummaryDto asDto(@NotNull DataCollectionEvent dce) {
    return Mica.DataCollectionEventSummaryDto.newBuilder().setId(dce.getId()) //
      .addAllName(localizedStringDtos.asDto(dce.getName())).build();
  }

  @NotNull
  Mica.StudySummaryDto asDto(@NotNull Study study) {
    return asDtoBuilder(study).build();
  }

  @NotNull
  Mica.StudySummaryDto asDto(@NotNull Study study, @NotNull StudyState studyState) {
    Mica.EntityStateDto.Builder stateBuilder = Mica.EntityStateDto.newBuilder()
      .setRevisionsAhead(studyState.getRevisionsAhead()) //
      .setRevisionStatus(studyState.getRevisionStatus().name());

    if(studyState.isPublished()) {
      stateBuilder.setPublishedTag(studyState.getPublishedTag());
      if(studyState.hasPublishedId()) {
        stateBuilder.setPublishedId(studyState.getPublishedId());
      }
      if(studyState.hasPublicationDate()) {
        stateBuilder.setPublicationDate(studyState.getPublicationDate().toString());
        stateBuilder.setPublishedBy(studyState.getPublishedBy());
      }
    }

    stateBuilder.setPermissions(permissionsDtos.asDto(studyState));

    Mica.StudySummaryDto.Builder builder;

    if(study == null) {
      builder = Mica.StudySummaryDto.newBuilder();
      builder.setId(studyState.getId())
        .setTimestamps(TimestampsDtos.asDto(studyState));
    } else {
      builder = asDtoBuilder(study, studyState.isPublished(), 0);
    }

    builder.setPublished(studyState.isPublished());

    return builder.setExtension(Mica.EntityStateDto.studySummaryState, stateBuilder.build()).build();
  }

  @NotNull
  Mica.StudySummaryDto asDto(@NotNull StudyState studyState) {
    Study study = studyService.findStudy(studyState.getId());
    return asDto(study, studyState);
  }

  Mica.StudySummaryDto asDto(String studyId) {
    StudyState studyState = studyService.getEntityState(studyId);

    if(studyState.isPublished()) {
      Study study = publishedStudyService.findById(studyId);
      if(study != null) return asDtoBuilder(study, true, datasetVariableService.getCountByStudyId(studyId)).build();
    }

    return asDto(studyState);
  }
}
