/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations.helper;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.inject.Inject;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.search.aggregations.AggregationMetaDataProvider;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

@Component
public class DceIdAggregationMetaDataHelper extends AbstractIdAggregationMetaDataHelper {

  @Inject
  PublishedStudyService publishedStudyService;

  @Cacheable(value = "aggregations-metadata", key = "'dce'")
  public Map<String, AggregationMetaDataProvider.LocalizedMetaData> getDces() {
    List<Study> studies = publishedStudyService.findAll();
    Map<String, AggregationMetaDataProvider.LocalizedMetaData> res = Maps.newHashMap();

    studies.forEach(study -> {
      SortedSet<Population> pops = study.getPopulations();
      if(pops == null) return;
      pops.forEach(population -> {
        SortedSet<DataCollectionEvent> dces = population.getDataCollectionEvents();
        if(dces == null) return;

        dces.forEach(dce -> {
          MonikerData md = new MonikerData(study.getAcronym(), population, dce);

          LocalizedString title = new LocalizedString();
          study.getAcronym().entrySet().forEach(e -> title.put(e.getKey(), md.getTitle(e.getKey())));

          LocalizedString description = new LocalizedString();
          study.getAcronym().entrySet().forEach(e -> description.put(e.getKey(), md.getDescription(e.getKey())));

          res.put(StudyTable.getDataCollectionEventUId(study.getId(), population.getId(), dce.getId()),
            new AggregationMetaDataProvider.LocalizedMetaData(title, description));
        });
      });
    });

    return res;
  }

  @Override
  protected Map<String, AggregationMetaDataProvider.LocalizedMetaData> getIdAggregationMap() {
    return getDces();
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
      dceName = dce.getName();
      populationDescription = population.getDescription();
      dceDescription = dce.getDescription();
    }

    public String getTitle(String locale) {
      return String.format("%s:%s:%s", studyAcronym.get(locale), populationName.get(locale), dceName.get(locale));
    }

    public String getDescription(String locale) {
      return String.format("%s:%s:%s", studyAcronym.get(locale), populationDescription != null ? populationDescription.get(locale) : "",
        dceDescription != null ? dceDescription.get(locale) : "");
    }
  }

}
