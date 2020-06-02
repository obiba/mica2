package org.obiba.mica.dataset.rest.collection;

import org.obiba.mica.EntityIndexHealthResource;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Path("/collected-datasets/index/health")
@Scope("request")
public class CollectedDatasetsIndexHealthResource extends EntityIndexHealthResource<StudyDataset> {

  final private CollectedDatasetService collectedDatasetService;

  final private PublishedDatasetService publishedDatasetService;

  @Inject
  public CollectedDatasetsIndexHealthResource(CollectedDatasetService collectedDatasetService,
                                              PublishedDatasetService publishedDatasetService) {
    this.collectedDatasetService = collectedDatasetService;
    this.publishedDatasetService = publishedDatasetService;
  }

  @Override
  public Map<String, StudyDataset> findRequireIndexingInternal(String locale) {
    Map<String, StudyDataset> requireIndexing = super.findRequireIndexingInternal(locale);
    collectedDatasetService.findAllRequireIndexing().forEach(dataset -> requireIndexing.put(dataset.getId(), dataset));
    return requireIndexing;
  }

  @Override
  protected List<StudyDataset> findAllPublished() {
    return collectedDatasetService.findAllPublishedDatasets();
  }


  @Override
  protected List<String> findAllIndexedIds() {
    return publishedDatasetService.suggest(MAX_VALUE, "en", createEsQuery(StudyDataset.class), ES_QUERY_FIELDS);
  }

  @Override
  protected String getEntityTitle(StudyDataset entity, String locale) {
    return entity.getAcronym().get(locale);
  }
}
