package org.obiba.mica.dataset.rest.collection;

import org.obiba.mica.EntityIndexHealthResource;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Path("/collected-datasets/index/health")
@Scope("request")
public class CollectedDatasetsIndexHealthResource extends EntityIndexHealthResource<StudyDataset> {

  final private CollectedDatasetService collectedDatasetService;

  final private StudyDatasetStateRepository studyDatasetStateRepository;

  final private PublishedDatasetService publishedDatasetService;

  @Inject
  public CollectedDatasetsIndexHealthResource(CollectedDatasetService collectedDatasetService,
                                              StudyDatasetStateRepository studyDatasetStateRepository,
                                              PublishedDatasetService publishedDatasetService) {
    this.collectedDatasetService = collectedDatasetService;
    this.studyDatasetStateRepository = studyDatasetStateRepository;
    this.publishedDatasetService = publishedDatasetService;
  }

  @Override
  public Map<String, StudyDataset> findRequireIndexingInternal() {
    Map<String, StudyDataset> requireIndexing = super.findRequireIndexingInternal();
    collectedDatasetService.findAllRequireIndexing().forEach(dataset -> requireIndexing.put(dataset.getId(), dataset));
    return requireIndexing;
  }

  @Override
  protected List<StudyDataset> findAllPublished() {
    List<String> ids = studyDatasetStateRepository.findByPublishedTagNotNull()
      .stream()
      .map(StudyDatasetState::getId)
      .collect(Collectors.toList());
    return collectedDatasetService.findAllDatasets(ids);
  }

  @Override
  protected List<String> findAllIndexedIds() {
    return publishedDatasetService.suggest(MAX_VALUE, "en", createEsQuery(StudyDataset.class), ES_QUERY_FIELDS, null);
  }

  @Override
  protected LocalizedString getEntityTitle(StudyDataset entity) {
    return entity.getAcronym();
  }
}
