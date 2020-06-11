package org.obiba.mica.study.rest;

import org.obiba.mica.EntityIndexHealthResource;
import org.obiba.mica.study.StudyStateRepository;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/individual-studies/index/health")
@Scope("request")
public class IndividualStudiesIndexHealthResource extends EntityIndexHealthResource<Study> {

  final private IndividualStudyService individualStudyService;

  final private StudyStateRepository studyStateRepository;

  final private PublishedStudyService publishedStudyService;

  @Inject
  public IndividualStudiesIndexHealthResource(IndividualStudyService individualStudyService,
                                              StudyStateRepository studyStateRepository,
                                              PublishedStudyService publishedStudyService) {
    this.individualStudyService = individualStudyService;
    this.studyStateRepository = studyStateRepository;
    this.publishedStudyService = publishedStudyService;
  }


  @Override
  protected List<Study> findAllPublished() {
    List<String> ids = studyStateRepository.findByPublishedTagNotNull()
      .stream()
      .map(StudyState::getId)
      .collect(Collectors.toList());
    return individualStudyService.findAllPublishedStudies(ids);
  }

  @Override
  protected List<String> findAllIndexedIds() {
    return publishedStudyService.suggest(MAX_VALUE, "en", createEsQuery(Study.class), ES_QUERY_FIELDS, null);
  }

  @Override
  protected String getEntityTitle(Study entity, String locale) {
    return entity.getAcronym().get(locale);
  }
}
