package org.obiba.mica.access.rest;


import com.codahale.metrics.annotation.Timed;
import com.google.common.eventbus.EventBus;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessAmendmentService;
import org.obiba.mica.security.event.ResourceDeletedEvent;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Map;

@Component
@Scope("request")
public class DataAccessAmendmentResource {

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private EventBus eventBus;

  @Inject
  private DataAccessAmendmentService dataAccessAmendmentService;

  private String parentId;

  private String id;

  @GET
  @Timed
  public Mica.DataAccessRequestDto getAmendment() {
    subjectAclService.checkPermission(String.format("/data-access-request/%s/amendment", parentId), "VIEW", id);
    DataAccessAmendment amendment = dataAccessAmendmentService.findById(id);
    return dtos.asAmendentDto(amendment);
  }

  @GET
  @Path("/model")
  @Produces("application/json")
  public Map<String, Object> getModel() {
    subjectAclService.checkPermission(String.format("/data-access-request/%s/amendment", parentId), "VIEW", id);
    return JSONUtils.toMap(dataAccessAmendmentService.findById(id).getContent());
  }

  @PUT
  @Timed
  public Response update(Mica.DataAccessRequestDto dto) {
    subjectAclService.checkPermission(String.format("/data-access-request/%s/amendment", parentId), "EDIT", id);
    if(!id.equals(dto.getId())) throw new BadRequestException();
    DataAccessAmendment request = dtos.fromAmendmentDto(dto);
    dataAccessAmendmentService.save(request);
    return Response.noContent().build();
  }

  @DELETE
  public Response delete() {
    String resource = String.format("/data-access-request/%s/amendment", parentId);
    subjectAclService.checkPermission(resource, "DELETE", id);

    try {
      dataAccessAmendmentService.delete(id);
      // remove associated comments
      eventBus.post(new ResourceDeletedEvent(resource, id));
      eventBus.post(new ResourceDeletedEvent(resource + "/" + id, "_status"));
    } catch(NoSuchDataAccessRequestException e) {
      // ignore
    }

    return Response.noContent().build();
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }
}
