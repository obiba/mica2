package org.obiba.mica.study.service;

import com.google.common.collect.Lists;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.service.DocumentSetService;
import org.obiba.mica.study.domain.BaseStudy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Validated
public class StudySetService extends DocumentSetService {

  private final PublishedStudyService publishedStudyService;

  @Inject
  public StudySetService(PublishedStudyService publishedStudyService) {
    this.publishedStudyService = publishedStudyService;
  }

  @Override
  public String getType() {
    return BaseStudy.MAPPING_NAME;
  }

  /**
   * Get a subset of the variables referred by the {@link DocumentSet}.
   *
   * @param documentSet
   * @param from
   * @param limit
   * @return
   */
  public List<BaseStudy> getPublishedStudies(DocumentSet documentSet, int from, int limit) {
    ensureType(documentSet);
    if (documentSet.getIdentifiers().isEmpty()) return Lists.newArrayList();
    List<String> ids = Lists.newArrayList(documentSet.getIdentifiers());
    Collections.sort(ids);
    int to = from + limit;
    if (to > ids.size()) to = ids.size();
    return publishedStudyService.findByIds(ids.subList(from, to));
  }

  /**
   * Get studies from their identifiers.
   *
   * @param identifiers
   * @param useCache
   * @return
   */
  public List<BaseStudy> getPublishedStudies(Set<String> identifiers, boolean useCache) {
    return publishedStudyService.findByIds(Lists.newArrayList(identifiers), useCache);
  }

  /**
   * Get the variables referred by the {@link DocumentSet}.
   *
   * @param documentSet
   * @return
   */
  public List<BaseStudy> getPublishedStudies(DocumentSet documentSet, boolean useCache) {
    return getPublishedStudies(documentSet.getIdentifiers(), useCache);
  }
}
