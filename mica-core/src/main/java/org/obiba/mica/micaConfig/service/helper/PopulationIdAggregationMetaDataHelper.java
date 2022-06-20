package org.obiba.mica.micaConfig.service.helper;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;

import static org.obiba.mica.security.SubjectUtils.sudo;

@Component
public class PopulationIdAggregationMetaDataHelper extends AbstractStudyAggregationMetaDataHelper {

  private final PublishedStudyService publishedStudyService;

  @Inject
  public PopulationIdAggregationMetaDataHelper(PublishedStudyService publishedStudyService) {
    this.publishedStudyService = publishedStudyService;
  }

  @Override
  protected Map<String, AggregationMetaDataProvider.LocalizedMetaData> getIdAggregationMap() {
    return getPopulations();
  }

  @Cacheable(value = "aggregations-metadata", key = "'population'")
  public Map<String, AggregationMetaDataProvider.LocalizedMetaData> getPopulations() {
    List<BaseStudy> studies = sudo(publishedStudyService::findAll);

    Map<String, AggregationMetaDataProvider.LocalizedMetaData> map = new HashMap<>();

    studies.forEach(study -> {
      SortedSet<Population> populations = study.getPopulations();
      if (populations != null) {
        populations.forEach(population -> {
          LocalizedString name = population.getName();
          LocalizedString description = population.getDescription();
          map.put(
            StudyTable.getPopulationUId(study.getId(), population.getId()),
            new AggregationMetaDataProvider.LocalizedMetaData(name == null ? new LocalizedString() : name, description == null ? new LocalizedString() : description, population.getClass().getSimpleName()));
        });
      }
    });

    return map;
  }
}
