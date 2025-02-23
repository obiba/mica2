package org.obiba.mica.study.rest;

import org.obiba.mica.EntityIndexHealthResource;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.study.HarmonizationStudyStateRepository;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.HarmonizationStudyEntityState;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/harmonization-studies/index/health")
@Scope("request")
public class HarmonizationStudiesIndexHealthResource extends EntityIndexHealthResource<HarmonizationStudy> {

  final private HarmonizationStudyService harmonizationStudyService;

  final private HarmonizationStudyStateRepository harmonizationStudyStateRepository;

  final private PublishedStudyService publishedStudyService;

  @Inject
  public HarmonizationStudiesIndexHealthResource(HarmonizationStudyService harmonizationStudyService,
                                                 HarmonizationStudyStateRepository harmonizationStudyStateRepository,
                                                 PublishedStudyService publishedStudyService) {
    this.harmonizationStudyService = harmonizationStudyService;
    this.harmonizationStudyStateRepository = harmonizationStudyStateRepository;
    this.publishedStudyService = publishedStudyService;
  }

  @Override
  protected List<HarmonizationStudy> findAllPublished() {
    List<String> ids = harmonizationStudyStateRepository.findByPublishedTagNotNull()
      .stream()
      .map(HarmonizationStudyEntityState::getId)
      .collect(Collectors.toList());
    return harmonizationStudyService.findAllPublishedStudies(ids);
  }

  @Override
  protected List<String> findAllIndexedIds() {
    return publishedStudyService.suggest(MAX_VALUE, "en", createEsQuery(HarmonizationStudy.class), ES_QUERY_FIELDS, null);
  }

  @Override
  protected LocalizedString getEntityTitle(HarmonizationStudy entity) {
    return entity.getAcronym();
  }
}
