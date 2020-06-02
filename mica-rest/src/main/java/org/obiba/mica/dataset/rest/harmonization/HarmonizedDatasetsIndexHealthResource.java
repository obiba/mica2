package org.obiba.mica.dataset.rest.harmonization;

import com.google.common.collect.Lists;
import org.obiba.mica.EntityIndexHealthResource;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.List;

@Component
@Path("/harmonized-datasets/index/health")
@Scope("request")
public class HarmonizedDatasetsIndexHealthResource extends EntityIndexHealthResource<HarmonizationDataset> {

  final private HarmonizedDatasetService harmonizedDatasetService;

  final private PublishedDatasetService publishedDatasetService;

  @Inject
  public HarmonizedDatasetsIndexHealthResource(HarmonizedDatasetService harmonizedDatasetService,
                                               PublishedDatasetService publishedDatasetService) {
    this.harmonizedDatasetService = harmonizedDatasetService;
    this.publishedDatasetService = publishedDatasetService;
  }


  @Override
  protected List<HarmonizationDataset> findAllPublished() {
    return harmonizedDatasetService.findAllPublishedDatasets();
  }


  @Override
  protected List<String> findAllIndexedIds() {
    return publishedDatasetService.suggest(MAX_VALUE, "en", createEsQuery(HarmonizationDataset.class), ES_QUERY_FIELDS);
  }

  @Override
  protected String getEntityTitle(HarmonizationDataset entity, String locale) {
    return entity.getAcronym().get(locale);
  }
}
