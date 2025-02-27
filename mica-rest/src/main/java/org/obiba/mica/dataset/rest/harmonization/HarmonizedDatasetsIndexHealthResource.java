package org.obiba.mica.dataset.rest.harmonization;

import org.obiba.mica.EntityIndexHealthResource;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/harmonized-datasets/index/health")
@Scope("request")
public class HarmonizedDatasetsIndexHealthResource extends EntityIndexHealthResource<HarmonizationDataset> {

  final private HarmonizedDatasetService harmonizedDatasetService;

  final private HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  final private PublishedDatasetService publishedDatasetService;

  @Inject
  public HarmonizedDatasetsIndexHealthResource(HarmonizedDatasetService harmonizedDatasetService,
                                               HarmonizationDatasetStateRepository harmonizationDatasetStateRepository,
                                               PublishedDatasetService publishedDatasetService) {
    this.harmonizedDatasetService = harmonizedDatasetService;
    this.harmonizationDatasetStateRepository = harmonizationDatasetStateRepository;
    this.publishedDatasetService = publishedDatasetService;
  }


  @Override
  protected List<HarmonizationDataset> findAllPublished() {
    List<String> ids = harmonizationDatasetStateRepository.findByPublishedTagNotNull()
      .stream()
      .map(HarmonizationDatasetState::getId)
      .collect(Collectors.toList());

    return harmonizedDatasetService.findPublishedDatasets(ids);
  }

  @Override
  protected List<String> findAllIndexedIds() {
    return publishedDatasetService.suggest(MAX_VALUE, "en", createEsQuery(HarmonizationDataset.class), ES_QUERY_FIELDS, null);
  }

  @Override
  protected LocalizedString getEntityTitle(HarmonizationDataset entity) {
    return entity.getAcronym();
  }
}
