package org.obiba.mica.search.aggregations;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.inject.Inject;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

@Component
public class DataCollectionEventAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final String AGGREGATION_NAME = "dceIds";

  @Inject
  AggregationMetaDataHelper helper;

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    Map<String, AggregationMetaDataProvider.LocalizedMetaData> dceDictionary = helper.getDces();

    return AGGREGATION_NAME.equals(aggregation) && dceDictionary.containsKey(termKey)
      ? MetaData.newBuilder()
          .title(dceDictionary.get(termKey).getTitle().get(locale))
          .description(dceDictionary.get(termKey).getDescription().get(locale)).build()
      : null;
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    return AGGREGATION_NAME.equals(aggregation);
  }

  @Override
  public void refresh() {
  }

  @Component
  public static class AggregationMetaDataHelper {

    @Inject
    PublishedStudyService publishedStudyService;

    @Cacheable(value = "aggregations-metadata", key = "'dce'")
    public Map<String, LocalizedMetaData> getDces() {
      List<Study> studies = publishedStudyService.findAll();
      Map<String, LocalizedMetaData> res = Maps.newHashMap();

      studies.forEach(study -> {
        SortedSet<Population> pops = study.getPopulations();
        if(pops == null) return;
        pops.forEach(population -> {
          SortedSet<DataCollectionEvent> dces = population.getDataCollectionEvents();
          if(dces == null) return;

          dces.forEach(dce -> {
            MonikerData md = new MonikerData(population.getId(), dce.getId(), study.getAcronym(), population.getName(),
              dce.getName());

            LocalizedString n = new LocalizedString();
            study.getAcronym().entrySet().forEach(e -> n.put(e.getKey(), md.getTitle(e.getKey())));

            LocalizedString m = new LocalizedString();
            study.getAcronym().entrySet().forEach(e -> m.put(e.getKey(), md.getDescription(e.getKey())));

            res.put(StudyTable.getDataCollectionEventUId(study.getId(), population.getId(), dce.getId()),
              new LocalizedMetaData(n, m));
          });
        });
      });

      return res;
    }
  }

  public static class MonikerData {
    LocalizedString studyAcronym;
    LocalizedString populationName;
    LocalizedString dceName;
    String populationId;
    String dceId;

    public MonikerData(String populationId, String dceId, LocalizedString studyAcronym, LocalizedString populationName, LocalizedString dceName) {
      this.studyAcronym = studyAcronym;
      this.populationName = populationName;
      this.dceName = dceName;
      this.populationId = populationId;
      this.dceId = dceId;
    }

    public String getTitle(String locale) {
      return String.format("%s:%s:%s", studyAcronym.get(locale), populationId, dceId);
    }

    public String getDescription(String locale) {
      return String.format("%s:%s:%s", studyAcronym.get(locale), populationName.get(locale),
        dceName.get(locale));
    }
  }
}
