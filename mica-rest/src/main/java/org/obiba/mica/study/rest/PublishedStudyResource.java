/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.ModelAwareTranslator;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing Study.
 */
@Component
@Path("/study/{id}")
@Scope("request")
@RequiresAuthentication
public class PublishedStudyResource {

  private static final Logger log = LoggerFactory.getLogger(PublishedStudyResource.class);

  @Inject
  private FileSystemService fileSystemService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private ModelAwareTranslator modelAwareTranslator;

  @GET
  @Timed
  public Mica.StudyDto get(@PathParam("id") String id, @QueryParam("locale") String locale) {
    checkAccess(id);
    return dtos.asDto(getStudy(id, locale));
  }

  @Path("/file/{fileId}")
  public FileResource study(@PathParam("id") String id, @PathParam("fileId") String fileId) {
    checkAccess(id);
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    Study study = getStudy(id);
    if(study.hasLogo() && study.getLogo().getId().equals(fileId)) {
      fileResource.setAttachment(study.getLogo());
    } else {
      List<Attachment> attachments = fileSystemService
        .findAttachments(String.format("^/study/%s", study.getId()), true).stream()
        .filter(a -> a.getId().equals(fileId)).collect(Collectors.toList());
      if(attachments.isEmpty()) throw NoSuchEntityException.withId(Attachment.class, fileId);
      fileResource.setAttachment(attachments.get(0));
    }

    return fileResource;
  }

  private void checkAccess(String id) {
    subjectAclService.checkAccess("/study", id);
  }

  private Study getStudy(String id) {
    return getStudy(id, null);
  }

  private Study getStudy(String id, String locale) {
    Study study = publishedStudyService.findById(id);

    if (study == null)
      throw NoSuchStudyException.withId(id);

    translateModels(locale, study);

    log.debug("Study acronym {}", study.getAcronym());

    return study;
  }

  private void translateModels(String locale, Study study) {

    ModelAwareTranslator.ForLocale modelTranslator = modelAwareTranslator.getModelAwareTranslatorForLocale(locale);

    modelTranslator.translateModel(study);
    study.getPopulations().forEach(modelTranslator::translateModel);
    study.getPopulations()
      .forEach(population -> population.getDataCollectionEvents()
        .forEach(modelTranslator::translateModel));
  }
}
