/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.NoSuchEntityException;
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

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing Study.
 */
@Component
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

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  public Mica.StudyDto get() {
    checkAccess();
    return dtos.asDto(getStudy());
  }

  @Path("/file/{fileId}")
  public FileResource study(@PathParam("fileId") String fileId) {
    checkAccess();
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    Study study = getStudy();
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

  private void checkAccess() {
    subjectAclService.checkAccess("/study", id);
  }

  private Study getStudy() {
    Study study = publishedStudyService.findById(id);
    log.debug("Study acronym {}", study.getAcronym());

    if(study == null) throw NoSuchStudyException.withId(id);
    return study;
  }
}
