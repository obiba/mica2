package org.obiba.mica.study.rest;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
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

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private Dtos dtos;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  public Mica.StudyDto get() {
    return dtos.asDto(getStudy());
  }

  @Path("/file/{fileId}")
  public FileResource study(@PathParam("fileId") String fileId) {
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    Study study = getStudy();

    if(study.findAttachmentById(fileId) == null) throw NoSuchEntityException.withId(Attachment.class, fileId);

    fileResource.setAttachment(study.findAttachmentById(fileId));

    return fileResource;
  }

  @GET
  @Path("/files")
  public List<Mica.AttachmentDto> listAttachments() {
    Study study = getStudy();
    return StreamSupport.stream(study.getAllAttachments().spliterator(), false).sorted().map(dtos::asDto)
      .collect(Collectors.toList());
  }

  private Study getStudy() {
    Study study = publishedStudyService.findById(id);
    if(study == null) throw NoSuchStudyException.withId(id);
    return study;
  }
}
