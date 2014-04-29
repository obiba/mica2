package org.obiba.mica.service;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Study;
import org.obiba.mica.domain.StudyState;
import org.obiba.mica.event.StudyUpdatedEvent;
import org.obiba.mica.repository.StudyStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.EventBus;

import static org.obiba.mica.domain.RevisionStatus.DRAFT;

@Service
public class StudyService {

  private static final Logger log = LoggerFactory.getLogger(StudyService.class);

  @Inject
  private StudyStateRepository studyStateRepository;

  @Inject
  private GitService gitService;

  @Inject
  private EventBus eventBus;

  public void save(@NotNull @Valid Study study) {
    StudyState studyState = findStudyState(study);
    gitService.save(studyState.getId(), study);

    studyState.setName(study.getName());
    studyStateRepository.save(studyState);

    eventBus.post(new StudyUpdatedEvent(study));
  }

  @NotNull
  private StudyState findStudyState(Study study) {
    if(study.getId() == null) {
      StudyState studyState = new StudyState();
      studyState.setName(study.getName());
      studyStateRepository.save(studyState);
      study.setId(studyState.getId());
      return studyState;
    }
    StudyState studyState = studyStateRepository.findOne(study.getId());
    if(studyState == null) throw NoSuchStudyException.withId(study.getId());
    return studyState;
  }

  @NotNull
  public StudyState findStateByStudy(@NotNull Study study) throws NoSuchStudyException {
    return findStateById(study.getId());
  }

  @NotNull
  public StudyState findStateById(@NotNull String id) throws NoSuchStudyException {
    StudyState studyState = studyStateRepository.findOne(id);
    if(studyState == null) throw NoSuchStudyException.withId(id);
    return studyState;
  }

  @NotNull
  public Study findDraftStudy(@NotNull String id) throws NoSuchStudyException {
    // ensure study exists
    findStateById(id);
    return gitService.readHead(id, Study.class);
  }

  @NotNull
  public Study findPublishedStudy(@NotNull String id) throws NoSuchStudyException {
    StudyState studyState = findStateById(id);
    return gitService.readFromTag(id, studyState.getPublishedTag(), Study.class);
  }

  public List<StudyState> findAllStates() {
    return studyStateRepository.findAll();
  }

  public List<StudyState> findPublishedStates() {
    return studyStateRepository.findByPublishedTagNotNull();
  }

//  public void delete(@NotNull String id) {
//    studyRepository.delete(id);
//  }

  /**
   * Publish current revision
   *
   * @param id
   * @return
   * @throws NoSuchStudyException
   */
  public StudyState publish(@NotNull String id) throws NoSuchStudyException {
    StudyState studyState = findStateById(id);
    studyState.setRevisionStatus(DRAFT);
    studyState.setPublishedTag(gitService.tag(id));
    studyStateRepository.save(studyState);
    return studyState;
  }
}
