package org.obiba.mica.search.aggregations;

import java.util.Map;
import java.util.SortedSet;

import javax.inject.Inject;

import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

@Component
public class DataCollectionEventAggregationMetaDataProvider implements AggregationMetaDataProvider {

  @Inject
  PublishedStudyService publishedStudyService;

  @Inject
  StudyService studyService;

  private Map<String, MonikerData> cache;

  public void refresh() {
    cache = Maps.newHashMap();
    publishedStudyService.findAll().forEach(
      study -> {
        SortedSet<Population> pops = study.getPopulations();
        if (pops == null) return;
        pops.forEach(population -> {
          SortedSet<DataCollectionEvent> dces = population.getDataCollectionEvents();
          if(dces == null) return;
          dces.forEach(dce -> cache
            .put(StudyTable.getDataCollectionEventUId(study.getId(), population.getId(), dce.getId()),
              MonikerData.newBuilder().study(study).population(population).dataCollectionEvent(dce).build()));
      });
    });
  }

  public MetaData getTitle(String aggregation, String termKey, String locale) {
    return "dceIds".equals(aggregation) && cache.containsKey(termKey)
      ? MetaData.newBuilder().title(cache.get(termKey).getTitle(locale)).description(
      cache.get(termKey).getDescription(locale)).build()
      : null;
  }

  private static class MonikerData {
    private Study study;
    private Population population;
    private DataCollectionEvent dataCollectionEvent;

    public static Builder newBuilder() {
      return new Builder();
    }

    public String getTitle(String locale) {
      return String.format("%s:%s:%s", study.getAcronym().get(locale), population.getId(),
        dataCollectionEvent.getId());
    }

    public String getDescription(String locale) {
      return String.format("%s:%s:%s", study.getAcronym().get(locale), population.getName().get(locale),
        dataCollectionEvent.getName().get(locale));
    }

    static class Builder {
      private MonikerData data = new MonikerData();

      public Builder study(Study value) {
        data.study = value;
        return this;
      }

      public Builder population(Population value) {
        data.population = value;
        return this;
      }

      public Builder dataCollectionEvent(DataCollectionEvent value) {
        data.dataCollectionEvent = value;
        return this;
      }

      public MonikerData build() {
        return data;
      }

    }

  }

}
