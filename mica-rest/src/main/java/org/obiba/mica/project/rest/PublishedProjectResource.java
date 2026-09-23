/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.rest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.service.NoSuchProjectException;
import org.obiba.mica.project.service.PublishedProjectService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing Network.
 */
@Component
@Scope("request")
@RequiresAuthentication
public class PublishedProjectResource {

  @Inject
  private PublishedProjectService publishedProjectService;

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private FileSystemService fileSystemService;

  @Inject
  private ApplicationContext applicationContext;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  public Mica.ProjectDto get() {
    checkAccess();
    return dtos.asDto(getProject());
  }

  @GET
  @Path("/model")
  @Produces("application/json")
  public Map<String, Object> getModel() {
    checkAccess();
    return getProject().getModel();
  }

  @Path("/file/{fileId}")
  public FileResource file(@PathParam("fileId") String fileId) {
    checkAccess();
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    List<Attachment> attachments = fileSystemService
      .findAttachments(String.format("^/project/%s", id), true).stream()
      .filter(a -> a.getId().equals(fileId)).collect(Collectors.toList());
    if (attachments.isEmpty()) throw NoSuchEntityException.withId(Attachment.class, fileId);
    fileResource.setAttachment(attachments.get(0));
    return fileResource;
  }

  private void checkAccess() {
    subjectAclService.checkAccess("/project", id);
  }

  private Project getProject() {
    Project project = publishedProjectService.findById(id);
    if (project == null) throw NoSuchProjectException.withId(id);
    return project;
  }
}
