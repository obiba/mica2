package org.obiba.mica.service;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.study.Study;
import org.obiba.mica.repository.StudyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StudyServiceImpl {

  private static final Logger log = LoggerFactory.getLogger(StudyServiceImpl.class);

  @Inject
  private StudyRepository studyRepository;

  public void save(@NotNull Study study) {
    studyRepository.save(study);
  }

  @NotNull
  public Study findById(@NotNull String id) throws NoSuchStudyException {
    Study study = studyRepository.findOne(id);
    if(study == null) throw NoSuchStudyException.withId(id);
    return study;
  }

  @NotNull
  public Study findByName(@NotNull String name) throws NoSuchStudyException {
    Study study = studyRepository.findByName(name);
    if(study == null) throw NoSuchStudyException.withName(name);
    return study;
  }

}
