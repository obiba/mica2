/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service.helper;

import com.google.common.collect.Maps;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.study.domain.*;
import org.obiba.mica.study.service.PublishedStudyService;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static org.obiba.mica.security.SubjectUtils.sudo;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DceIdAggregationMetaDataHelper extends AbstractStudyAggregationMetaDataHelper {

  private static final Logger log = getLogger(DceIdAggregationMetaDataHelper.class);

  @Inject
  PublishedStudyService publishedStudyService;

  @Cacheable(value = "aggregations-metadata", key = "'dce'")
  public Map<String, AggregationMetaDataProvider.LocalizedMetaData> getDces() {
    try {
      List<BaseStudy> studies = sudo(() -> publishedStudyService.findAllByClassName(Study.class.getSimpleName()));
      Map<String, AggregationMetaDataProvider.LocalizedMetaData> map = Maps.newHashMap();

      studies.forEach(study -> {
        SortedSet<Population> pops = study.getPopulations();
        if (pops == null) return;
        pops.forEach(population -> {
          SortedSet<DataCollectionEvent> dces = population.getDataCollectionEvents();
          if (dces == null) return;
          dces.forEach(dce -> mapIdToMetadata(map, study, population, dce));
        });
      });

      sudo(() -> publishedStudyService.findAllByClassName(HarmonizationStudy.class.getSimpleName()))
          .stream()
          .forEach(hStudy -> hStudy.getPopulations().stream()
              .forEach(population -> mapIdToMetadata(map, hStudy, population, null)));

      return map;
    } catch (Exception e) {
      log.debug("Could not build DCE aggregation metadata {}", e);
      return Maps.newHashMap();
    }
  }

  @Override
  protected Map<String, AggregationMetaDataProvider.LocalizedMetaData> getIdAggregationMap() {
    return getDces();
  }

  private void mapIdToMetadata(Map<String, AggregationMetaDataProvider.LocalizedMetaData> map,
                               BaseStudy study, Population population, DataCollectionEvent dce) {

    MonikerData md = new MonikerData(study.getAcronym(), population, dce);

    LocalizedString title = new LocalizedString();
    study.getAcronym().entrySet().forEach(e -> title.put(e.getKey(), md.getTitle(e.getKey())));

    LocalizedString description = new LocalizedString();
    study.getAcronym().entrySet().forEach(e -> description.put(e.getKey(
    ), md.getDescription(e.getKey())));

    if (dce == null) {
      String sortField = study.getId() + ":" + String.format("%04d", population.getWeight());
      map.put(StudyTable.getDataCollectionEventUId(study.getId(), population.getId()),
          new AggregationMetaDataProvider.LocalizedMetaData(title, description, "", null, null, sortField));
    } else {
      String start = dce.hasStart() ? dce.getStart().getYearMonth() : null;
      String end = dce.hasEnd() ? dce.getEnd().getYearMonth() : null;

      String sortField = study.getId() + ":" + String.format("%04d", population.getWeight()) + ":" + String.format("%04d", dce.getWeight());

      map.put(
          StudyTable.getDataCollectionEventUId(study.getId(), population.getId(), dce.getId()),
          new AggregationMetaDataProvider.LocalizedMetaData(title,
              description,
              dce.getClass().getSimpleName(),
              start,
              end,
              sortField));
    }
  }

  static class MonikerData {
    LocalizedString studyAcronym;
    LocalizedString populationName;
    LocalizedString dceName;
    LocalizedString populationDescription;
    LocalizedString dceDescription;

    MonikerData(LocalizedString studyAcronym, Population population, DataCollectionEvent dce) {
      this.studyAcronym = studyAcronym;
      populationName = population.getName();
      populationDescription = population.getDescription();
      if (dce != null) {
        dceName = dce.getName();
        dceDescription = dce.getDescription();
      }
    }

    public String getTitle(String locale) {
      return dceName == null
          ? String.format("%s:%s",
          studyAcronym.getOrDefault(locale, ""),
          populationName.getOrDefault(locale, ""))
          : String.format("%s:%s:%s",
          studyAcronym.getOrDefault(locale, ""),
          populationName.getOrDefault(locale, ""),
          dceName.getOrDefault(locale, ""));
    }

    public String getDescription(String locale) {
      return String.format("%s:%s:%s",
          studyAcronym.getOrDefault(locale, ""),
          populationDescription != null ? populationDescription.getOrDefault(locale, "") : "",
          dceDescription != null ? dceDescription.getOrDefault(locale, "") : "");
    }
  }
}

