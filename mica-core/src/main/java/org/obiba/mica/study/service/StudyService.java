/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

@Component
public class StudyService {

  @Inject
  protected EventBus eventBus;

  @Inject
  private CollectionStudyService collectionStudyService;

  @Inject
  private HarmonizationStudyService harmonizationStudyService;

  public void indexAll() {
    eventBus.post(new IndexStudiesEvent());
  }

  public boolean isCollectionStudy(String id) {
    try {
      return findStudy(id) instanceof Study;
    } catch(NoSuchEntityException ex) {
      return false;
    }
  }

  public void save(@NotNull @Valid BaseStudy study, @Nullable String comment) {
    if (study instanceof Study) {
      collectionStudyService.save((Study)study, comment);
    } else {
      harmonizationStudyService.save((HarmonizationStudy)study, comment);
    }
  }

  public void publish(@NotNull String id, boolean publish, PublishCascadingScope cascadingScope)
    throws NoSuchEntityException {

    if (isCollectionStudy(id)) {
      collectionStudyService.publish(id, publish, cascadingScope);
    } else {
      harmonizationStudyService.publish(id, publish, cascadingScope);
    }
  }

  public boolean isHarmonizationStudy(String id) {
    return !isCollectionStudy(id);
  }

  public EntityState getEntityState(String id) throws NoSuchEntityException {
    try {
      return collectionStudyService.getEntityState(id);
    } catch(NoSuchEntityException ex) {
      return harmonizationStudyService.getEntityState(id);
    }
  }

  public EntityState findStateById(String id) throws NoSuchEntityException {
    EntityState state = collectionStudyService.findStateById(id);
    if (state == null) state = harmonizationStudyService.findStateById(id);
    return state;
  }

  public BaseStudy findDraft(String id) throws NoSuchEntityException {
    try {
      return collectionStudyService.findDraft(id);
    } catch(NoSuchEntityException ex) {
      return harmonizationStudyService.findDraft(id);
    }
  }

  public BaseStudy findDraft(@NotNull String id, String locale) throws NoSuchEntityException {
    try {
      return collectionStudyService.findDraft(id, locale);
    } catch(NoSuchEntityException ex) {
      return harmonizationStudyService.findDraft(id, locale);
    }
  }

  @NotNull
  public BaseStudy findStudy(@NotNull String id) throws NoSuchEntityException {
    try {
      return collectionStudyService.findStudy(id);
    } catch(NoSuchEntityException ex) {
      return harmonizationStudyService.findStudy(id);
    }
  }

  public boolean isPublished(@NotNull String id) throws NoSuchEntityException {
    try {
      return collectionStudyService.getEntityState(id).isPublished();
    } catch(NoSuchEntityException ex) {
      return harmonizationStudyService.getEntityState(id).isPublished();
    }
  }

  public List<BaseStudy> findAllPublishedStudies() {
    return Stream
      .concat(collectionStudyService.findAllPublishedStudies().stream(),
        harmonizationStudyService.findAllPublishedStudies().stream())
      .collect(Collectors.toList());
  }

  public List<BaseStudy> findAllDraftStudies() {
    return Stream
      .concat(collectionStudyService.findAllDraftStudies().stream(),
        harmonizationStudyService.findAllDraftStudies().stream())
      .collect(Collectors.toList());
  }

  public List<? extends EntityState> findAllStates() {
    return Stream.concat(collectionStudyService.findAllStates().stream(),
      harmonizationStudyService.findAllStates().stream())
      .collect(Collectors.toList());
  }

  public List<? extends EntityState> findAllStates(Iterable<String> ids) {
    return Stream.concat(collectionStudyService.findAllStates(ids).stream(),
      harmonizationStudyService.findAllStates(ids).stream())
      .collect(Collectors.toList());
  }
}
