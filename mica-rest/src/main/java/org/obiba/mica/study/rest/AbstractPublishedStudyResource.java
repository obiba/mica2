/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.ModelAwareTranslator;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;


public abstract class AbstractPublishedStudyResource {

  private static final Logger log = LoggerFactory.getLogger(AbstractPublishedStudyResource.class);

  @Inject
  private FileSystemService fileSystemService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private StudyService studyService;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  protected Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private ModelAwareTranslator modelAwareTranslator;

  protected FileResource getStudyFileResource(String id, String fileId) {
    checkAccess(id);
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    BaseStudy study = getStudy(id);
    if(study.hasLogo() && study.getLogo().getId().equals(fileId)) {
      fileResource.setAttachment(study.getLogo());
    } else {
      List<Attachment> attachments = fileSystemService
        .findAttachments(String.format("^/%s/%s", getStudyPath(id), id), true).stream()
        .filter(a -> a.getId().equals(fileId)).collect(Collectors.toList());
      if(attachments.isEmpty()) throw NoSuchEntityException.withId(Attachment.class, fileId);
      fileResource.setAttachment(attachments.get(0));
    }

    return fileResource;
  }

  // TODO: this will be refractored in another like StudyService...
  protected String getStudyPath(String id) {
    return studyService.isCollectionStudy(id) ? "individual-study" : "harmonization-study";
  }

  protected void checkAccess(String id) {
    subjectAclService.checkAccess("/"+getStudyPath(id), id);
  }

  protected BaseStudy getStudy(String id) {
    return getStudy(id, null);
  }

  protected BaseStudy getStudy(String id, String locale) {
    BaseStudy study = publishedStudyService.findById(id);

    if (study == null)
      throw NoSuchStudyException.withId(id);

    translateModels(locale, study);

    log.debug("Study acronym {}", study.getAcronym());

    return study;
  }

  protected void translateModels(String locale, BaseStudy study) {

    ModelAwareTranslator.ForLocale modelTranslator = modelAwareTranslator.getModelAwareTranslatorForLocale(locale);

    modelTranslator.translateModel(study);
    study.getPopulations().forEach(modelTranslator::translateModel);
    study.getPopulations()
      .forEach(population -> population.getDataCollectionEvents()
        .forEach(modelTranslator::translateModel));
  }
}
