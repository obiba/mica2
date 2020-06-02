package org.obiba.mica.study.rest;

import org.obiba.mica.EntityIndexHealthResource;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.List;

@Component
@Path("/harmonization-studies/index/health")
@Scope("request")
public class HarmonizationStudiesIndexHealthResource extends EntityIndexHealthResource<HarmonizationStudy> {

  final private HarmonizationStudyService harmonizationStudyService;

  final private PublishedStudyService publishedStudyService;

  @Inject
  public HarmonizationStudiesIndexHealthResource(HarmonizationStudyService harmonizationStudyService,
                                                 PublishedStudyService publishedStudyService) {
    this.harmonizationStudyService = harmonizationStudyService;
    this.publishedStudyService = publishedStudyService;
  }

  @Override
  protected List<HarmonizationStudy> findAllPublished() {
    return harmonizationStudyService.findAllPublishedStudies();
  }

  @Override
  protected List<String> findAllIndexedIds() {
    return publishedStudyService.suggest(MAX_VALUE, "en", createEsQuery(HarmonizationStudy.class), ES_QUERY_FIELDS);
  }

  @Override
  protected String getEntityTitle(HarmonizationStudy entity, String locale) {
    return entity.getAcronym().get(locale);
  }
}
